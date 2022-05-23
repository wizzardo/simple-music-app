import ProgressBar from "./ProgressBar";
import Button from "react-ui-basics/Button";
import React, {useEffect, useRef, useState} from "react";

import {Song, SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";
import {css} from "goober";
import {formatDuration} from "../utils/Helpers";
import {FlexRow} from "./SharedComponents";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import {classNames} from "react-ui-basics/Tools";
import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import * as PlayerStore from "../stores/PlayerStore";
import * as SettingsStore from "../stores/SettingsStore";
import NetworkService from "../services/NetworkService";
import WindowActiveStore from "../stores/WindowActiveStore";
import {useIsSafari} from "../utils/Hooks";


const load = (url, setAudio, localCache: SongLocalCacheDB, artist: string, album: string, name: string, isRetrying?: boolean) => {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'arraybuffer';

    request.onload = () => {
        let audioData = request.response;
        if (request.status != 200 || Number(request.getResponseHeader('Content-Length')) === 0) {
            console.error(url, request.status, request.responseText)
            if (!isRetrying) {
                load(url, setAudio, localCache, artist, album, name, true)
            }
            return
        }
        if (request.status != 200) {
            console.error(url, request.status, request.responseText)
            return
        }

        const contentType = request.getResponseHeader('Content-Type');
        const data = audioData.slice();

        let song = {
            url,
            name,
            album,
            artist,
            type: contentType,
            size: data.byteLength,
            dataId: 0,
            timesPlayed: 0,
            dateAdded: new Date().getTime(),
        };
        localCache.add(song, data)
        setAudio(song, data)
    }
    request.send();
};

const Player = ({}) => {
    const localCache = useLocalCache();
    const isSafari = useIsSafari()
    const artistsStore = useStore(ArtistsStore.store)
    const {format, bitrate} = useStore(SettingsStore.store)
    const {playing, position, queue, offset, volume} = useStore(PlayerStore.store)

    const [progress, setProgress] = useState(0)

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
    const song = album?.songs?.find(it => it.id === queuedSong?.songId);
    const duration = song?.duration / 1000

    const [audio] = useState<HTMLAudioElement>(() => new Audio())

    useEffect(() => {
        let updater;
        audio.addEventListener('pause', (e) => {
            console.log('on pause', e)
            // PlayerStore.setPlaying(false)
            // clearInterval(updater)
            PlayerStore.setOffset(audio.currentTime)
        });

        audio.addEventListener('play', (e) => {
            console.log('on play', e)
            PlayerStore.setPlaying(true)

            const {queue, position} = PlayerStore.store.get();

            const queuedSong = queue[position]
            const artist = ArtistsStore.store.get().map[queuedSong?.artistId];
            const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
            const song = album?.songs?.find(it => it.id === queuedSong?.songId);
            const duration = song?.duration / 1000


            if (navigator.mediaSession) {
                navigator.mediaSession.metadata = new MediaMetadata({
                    title: song.title,
                    album: album.name,
                    artist: artist.name,
                    artwork: !album.coverHash ? [] : [
                        {src: NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + album.coverPath, type: 'image/jpeg'}
                    ]
                });

                console.log('setPositionState', duration, audio.currentTime)
                navigator.mediaSession.setPositionState({
                    duration,
                    position: audio.currentTime > duration ? 0 : audio.currentTime,
                    playbackRate: 1,
                })
            }

            // if (navigator.mediaSession) {
            //     navigator.mediaSession.metadata = new MediaMetadata({
            //         title: song.title,
            //         album: album.name,
            //         artist: artist.name,
            //         artwork: !album.coverHash ? [] : [
            //             {src: NetworkService.baseurl + '/artists/' + artist.path + '/' + album.path + '/' + album.coverPath, type: 'image/jpeg'}
            //         ]
            //     });
            //     navigator.mediaSession.setPositionState({
            //         duration,
            //         position: audio.currentTime,
            //         playbackRate: 1,
            //
            //     })
            //     navigator.mediaSession.setActionHandler('previoustrack', () => {
            //         PlayerStore.prev()
            //     });
            //
            //     navigator.mediaSession.setActionHandler('nexttrack', () => {
            //         PlayerStore.next()
            //     });
            // }
            console.log('start updater', duration)

            // if (navigator.mediaSession) {
            //     navigator.mediaSession.setPositionState({duration, position: audio.currentTime, playbackRate: 1})
            // }

            clearInterval(updater)
            const interval = setInterval(() => {
                const playerState = PlayerStore.store.get();
                if (!playerState.playing) {
                    clearInterval(interval)
                    return
                }
                const position = audio.currentTime

                const progress = position / duration * 100
                if (!WindowActiveStore.get().hidden)
                    setProgress(Math.min(progress, 100))
                if (progress > 100) {
                    // audio.pause()
                    clearInterval(interval)
                    PlayerStore.next()
                }
            }, 1000 / 30);
            // setUpdater(interval)
            updater = interval
        });

        audio.addEventListener('loadeddata', async ev => {
            console.log('loadeddata', ev)
            const {playing, queue, position} = PlayerStore.store.get();
            if (!playing)
                return

            try {
                await audio.play()

                const queuedSong = queue[position]
                const artist = ArtistsStore.store.get().map[queuedSong?.artistId];
                const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
                const song = album?.songs?.find(it => it.id === queuedSong?.songId);
                const duration = song?.duration / 1000


                if (navigator.mediaSession) {
                    navigator.mediaSession.metadata = new MediaMetadata({
                        title: song.title,
                        album: album.name,
                        artist: artist.name,
                        artwork: !album.coverHash ? [] : [
                            {src: NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + album.coverPath, type: 'image/jpeg'}
                        ]
                    });
                    // navigator.mediaSession.setPositionState({
                    //     duration,
                    //     position: audio.currentTime > duration ? 0 : audio.currentTime,
                    //     playbackRate: 1,
                    // })
                    navigator.mediaSession.setActionHandler('previoustrack', () => {
                        PlayerStore.prev()
                    });

                    navigator.mediaSession.setActionHandler('nexttrack', () => {
                        PlayerStore.next()
                    });
                    navigator.mediaSession.setActionHandler('stop', () => {
                        console.log('mediaSession on stop')
                        PlayerStore.setPlaying(false)
                        PlayerStore.setOffset(0)
                        audio.pause()
                    });
                    navigator.mediaSession.setActionHandler('pause', () => {
                        console.log('mediaSession on pause')
                        PlayerStore.setPlaying(false)
                        PlayerStore.setOffset(audio.currentTime)
                        audio.pause()
                    });

                    navigator.mediaSession.setActionHandler('seekto', (e) => {
                        if (e.fastSeek && 'fastSeek' in audio)
                            audio.fastSeek(e.seekTime);
                        else
                            audio.currentTime = e.seekTime;

                        console.log('setPositionState', duration, audio.currentTime)

                        navigator.mediaSession.setPositionState({
                            duration,
                            position: audio.currentTime,
                            playbackRate: 1,
                        })
                    });
                }

            } catch (e) {
                console.error(e)
                PlayerStore.setPlaying(false)
                return;
            }

        })
    }, [])

    useEffect(() => {
        (async () => {
            if (!localCache)
                return
            if (!playing)
                return
            if (!song)
                return
            if (!audio)
                return

            const audioUrl = NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + song.id + '/' + format + '/' + bitrate
            if (audio && audio.dataset.audioUrl === audioUrl) {
                return
            }

            URL.revokeObjectURL(audio.dataset.objectUrl)

            const cachedSong = await localCache.songByUrl(audioUrl);
            console.log('songByUrl', cachedSong, audioUrl)

            const loadAudio = async (song: Song, data) => {
                console.log('decoding', song)
                if (!audio.paused) {
                    audio.pause()
                    console.log('paused')
                }

                if (data.byteLength === 0) {
                    PlayerStore.next()
                    return
                }

                const blob = new Blob([new Uint8Array(data, 0, data.byteLength)])
                if (isSafari) {
                    let sourceElement = document.createElement('source')
                    audio.childNodes[0] && audio.removeChild(audio.childNodes[0])
                    audio.appendChild(sourceElement)
                    sourceElement.src = audio.dataset.objectUrl = URL.createObjectURL(blob)
                    sourceElement.type = song.type
                } else {
                    audio.src = audio.dataset.objectUrl = URL.createObjectURL(blob)
                }

                // audio.srcObject = blob
                // audio['type'] = song.type
                audio.dataset.audioUrl = audioUrl
                audio.load()
            };

            if (!cachedSong) {
                console.log('downloading', audioUrl)
                load(audioUrl, loadAudio, localCache, artist.name, album.name, song.title)
            } else {
                const sd = await localCache.songData(cachedSong.dataId)
                if (!sd)
                    load(audioUrl, loadAudio, localCache, artist.name, album.name, song.title)
                else
                    loadAudio(cachedSong, sd.data)
            }
        })().catch(console.error)
    }, [localCache, playing, song?.track, audio])

    useEffect(() => {
        if (!playing)
            return
        if (!audio)
            return

        console.log('set offset', offset, playing)
        audio.currentTime = offset
        audio.play().catch(console.error)

    }, [offset, playing])

    useEffect(() => {
        if (!audio)
            return
        if (!Number.isFinite(volume))
            return

        audio.volume = volume

    }, [audio, volume])

    let isMobile = window.innerWidth <= 800;

    return <div className={css`
      position: fixed;
      bottom: 0px;
      left: 0px;
      right: 0px;
      user-select: none;
    `}>
        <div className={css`
          padding: 20px;
        `}>
            <div className={css`
              max-width: 600px;
              margin-left: auto;
              margin-right: auto;
            `}>
                <FlexRow>
                <span className={css`margin-right: 10px;`}>
                    {audio && formatDuration(duration * 1000 / 100 * progress)}
                </span>
                    <ProgressBar draggable={false}
                                 progress={progress}
                                 onClick={progress => {
                                     if (!audio)
                                         return

                                     let offset = duration / 100 * progress;
                                     PlayerStore.setOffset(offset)
                                     PlayerStore.setPlaying(true)
                                 }}
                    />
                    <span className={css`margin-left: 10px;`}>
                    {audio && formatDuration(duration * 1000)}
                </span>
                </FlexRow>

                <FlexRow className={css`
                  margin-top: 10px;
                `}>
                    {!isMobile && <VolumeControl/>}

                    <FlexRow className={css`
                      justify-content: center;
                      flex-grow: 1;
                    `}>
                        <Button className={classNames('gray', css`
                          .MaterialIcon {
                            font-size: 20px;
                            color: gray;
                          }
                        `)} flat round onClick={e => {
                            PlayerStore.prev()
                            PlayerStore.setOffset(0)
                            PlayerStore.setPlaying(true)
                        }}>
                            <MaterialIcon icon={'skip_previous'}/>
                        </Button>

                        <Button className={classNames('red', css`
                          padding: 10px !important;
                          height: unset;
                          margin-left: 20px;
                          margin-right: 20px;

                          .MaterialIcon {
                            font-size: 30px;
                            color: white;
                          }
                        `)} flat round onClick={e => {
                            audio?.pause()
                            queue.length && PlayerStore.setPlaying(!playing);
                        }}>
                            <MaterialIcon icon={!playing ? 'play_arrow' : 'pause'}/>
                        </Button>


                        <Button className={classNames('gray', css`
                          .MaterialIcon {
                            font-size: 20px;
                            color: gray;
                          }
                        `)} flat round onClick={e => {
                            PlayerStore.next()
                            PlayerStore.setOffset(0)
                            PlayerStore.setPlaying(true)
                        }}>
                            <MaterialIcon icon={'skip_next'}/>
                        </Button>
                    </FlexRow>
                </FlexRow>

            </div>
        </div>
    </div>
}

export default Player;

const VolumeControl = ({className}: { className?: any }) => {
    const {volume} = useStore(PlayerStore.store)
    const [isDragging, setIsDragging] = useState(false)

    useEffect(() => {
        if (isDragging) {
            let removeListeners: () => void;
            let upListener = ev => {
                setIsDragging(false)
                removeListeners()
            };
            removeListeners = () => {
                document.removeEventListener('mouseup', upListener)
            };
            document.addEventListener('mouseup', upListener)
            return removeListeners
        }
    }, [isDragging])
    return <FlexRow
        className={classNames(css`
          position: absolute;

          .MaterialIcon {
            color: grey;
            margin-right: 10px;
          }

          .ProgressBar {
            width: 0px;
            transition: width 0.2s ease-out;

            .progress:after {
              opacity: 0;
            }
          }

          &:hover, &.hover {
            .ProgressBar {
              width: 100px;

              .progress:after {
                opacity: 1;
              }
            }
          }
        `, className, isDragging && 'hover')}
        onMouseDown={e => setIsDragging(true)}>
        <MaterialIcon icon={'volume_up'}/>
        <ProgressBar draggable={true} progress={volume * 100} onClick={volume => PlayerStore.setVolume(volume / 100)}/>
    </FlexRow>
}