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
import * as DownloadQueueStore from "../stores/DownloadQueueStore";
import {pushLocation} from "react-ui-basics/router/HistoryTools";

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
    const [silence] = useState<HTMLAudioElement>(() => new Audio())

    useEffect(() => {
        if (!silence)
            return
        silence.src = '/static/silence.mp3';
        silence.loop = true;
    }, [silence])

    useEffect(() => {
        let updater;
        audio.addEventListener('pause', (e) => {
            const {queue, position} = PlayerStore.store.get();

            const queuedSong = queue[position]
            const artist = ArtistsStore.store.get().map[queuedSong?.artistId];
            const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
            const song = album?.songs?.find(it => it.id === queuedSong?.songId);
            const duration = song?.duration / 1000

            console.log(new Date().toISOString(), 'on pause', e, audio.currentTime, duration, duration - audio.currentTime)
            // PlayerStore.setPlaying(false)
            // clearInterval(updater)
            if (duration - audio.currentTime > 0.01) {
                PlayerStore.setPlayingAndOffset(false, audio.currentTime)
                silence.pause()
            }
        });
        audio.addEventListener('ended', (e) => {
            const {queue, position} = PlayerStore.store.get();

            const queuedSong = queue[position]
            const artist = ArtistsStore.store.get().map[queuedSong?.artistId];
            const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
            const song = album?.songs?.find(it => it.id === queuedSong?.songId);
            const duration = song?.duration / 1000

            console.log(new Date().toISOString(), 'on ended', e, audio.currentTime, duration, duration - audio.currentTime)

            console.log(new Date().toISOString(), 'PlayerStore.next()')
            PlayerStore.next()
        });

        audio.addEventListener('play', (e) => {
            console.log(new Date().toISOString(), 'on play', e)
            PlayerStore.setPlaying(true)
            silence.play() // otherwise bluetooth headphones makes a pop sound in between tracks

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

                console.log(new Date().toISOString(), 'setPositionState', duration, audio.currentTime)
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
            console.log(new Date().toISOString(), 'start updater', duration)

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
                if (progress >= 100) {
                    // audio.pause()
                    clearInterval(interval)
                    // console.log(new Date().toISOString(), 'PlayerStore.next()')
                    // PlayerStore.next()
                }
            }, 1000 / 30);
            // setUpdater(interval)
            updater = interval
        });

        audio.addEventListener('loadeddata', async ev => {
            console.log(new Date().toISOString(), 'loadeddata', ev)
            const {playing, queue, position} = PlayerStore.store.get();
            // if (!playing)
            //     return

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
                        console.log(new Date().toISOString(), 'mediaSession on stop')
                        PlayerStore.setPlayingAndOffset(false, 0)
                        audio.pause()
                    });
                    navigator.mediaSession.setActionHandler('pause', () => {
                        console.log(new Date().toISOString(), 'mediaSession on pause')
                        PlayerStore.setPlayingAndOffset(false, audio.currentTime)
                        audio.pause()
                    });
                    navigator.mediaSession.setActionHandler('play', () => {
                        console.log(new Date().toISOString(), 'mediaSession on play')
                        PlayerStore.setPlaying(true)
                    });

                    navigator.mediaSession.setActionHandler('seekto', (e) => {
                        if (e.fastSeek && 'fastSeek' in audio)
                            audio.fastSeek(e.seekTime);
                        else
                            audio.currentTime = e.seekTime;

                        console.log(new Date().toISOString(), 'setPositionState', duration, audio.currentTime)

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
            console.log(new Date().toISOString(), 'songByUrl', cachedSong, audioUrl)

            const loadAudio = async (song?: Song, data?: ArrayBuffer, attachAudio?: (audio: HTMLAudioElement) => void) => {
                console.log(new Date().toISOString(), 'decoding', song)
                if (!audio.paused) {
                    audio.pause()
                    console.log(new Date().toISOString(), 'paused')
                }

                if (attachAudio) {
                    attachAudio(audio)
                } else if (data) {
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
                } else {
                    audio.src = audioUrl + '/stream'
                }

                // audio.srcObject = blob
                // audio['type'] = song.type
                audio.dataset.audioUrl = audioUrl
                audio.load()
            };

            if (!cachedSong) {
                console.log(new Date().toISOString(), 'downloading', audioUrl)
                DownloadQueueStore.download(
                    audioUrl,
                    artist.name,
                    album.name,
                    song.title,
                    format,
                    bitrate,
                    loadAudio
                )
            } else {
                const sd = await localCache.songData(cachedSong.dataId)
                if (!sd) {
                    DownloadQueueStore.download(
                        audioUrl,
                        artist.name,
                        album.name,
                        song.title,
                        format,
                        bitrate,
                        loadAudio
                    )
                } else
                    loadAudio(cachedSong, sd.data)
            }
        })().catch(console.error)
    }, [localCache, playing, song, audio])

    useEffect(() => {
        if (!playing)
            return
        if (!audio)
            return

        console.log(new Date().toISOString(), 'set offset', offset, playing)
        audio.currentTime = offset
        audio.play().catch(console.error)

    }, [offset, playing])

    useEffect(() => {
        if (!audio)
            return
        if (!Number.isFinite(volume))
            return

        audio.volume = Math.max(0, Math.min(volume, 1))

    }, [audio, volume])

    useEffect(() => {
        if (playing)
            return
        if (!duration)
            return

        const progress = offset / duration * 100
        setProgress(Math.min(progress, 100))
    }, [playing, offset, duration])

    let isMobile = window.innerWidth <= 800;

    return <div className={css`
      position: fixed;
      bottom: 0px;
      left: 0px;
      right: 0px;
      user-select: none;
      height: 151px;
      overflow: hidden;
    `}>
        <div className={css`
          padding: 20px;
        `}>
            <div className={css`
              max-width: 600px;
              margin-left: auto;
              margin-right: auto;
            `}>
                {song && <FlexRow
                    className={css`
                      justify-content: center;
                      flex-wrap: wrap;
                      height: 33px;
                      cursor: pointer;
                    `}
                    onClick={e => {
                        pushLocation('/' + artist?.id + '/' + album?.path)
                    }}
                >
                    <b className={css`
                      white-space: nowrap;
                      overflow: hidden;
                      text-overflow: ellipsis;
                    `}>
                        {song.title}
                    </b>
                    <span className={css`
                      font-size: 12px;
                      white-space: nowrap;
                      overflow: hidden;
                      text-overflow: ellipsis;
                    `}>
                        &nbsp;&nbsp;by&nbsp;&nbsp;
                        {artist.name}
                    </span>
                </FlexRow>}
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
                            if (queue.length) {
                                if (audio && playing)
                                    PlayerStore.setPlayingAndOffset(!playing, audio.currentTime);
                                else
                                    PlayerStore.setPlaying(!playing);
                            }
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
    const {volume, playing} = useStore(PlayerStore.store)
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
            color: ${playing ? 'black' : 'grey'};
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