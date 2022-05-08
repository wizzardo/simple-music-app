import ProgressBar from "./ProgressBar";
import Button from "react-ui-basics/Button";
import React, {useEffect, useState} from "react";

import {SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";
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


const load = (url, setAudio, localCache: SongLocalCacheDB, artist: string, album: string, name: string) => {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'arraybuffer';

    request.onload = () => {
        let audioData = request.response;
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

    const artistsStore = useStore(ArtistsStore.store)
    const {format, bitrate} = useStore(SettingsStore.store)
    const {playing, position, queue, offset} = useStore(PlayerStore.store)

    const [progress, setProgress] = useState(0)
    const [updater, setUpdater] = useState<NodeJS.Timer>()

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
    const song = album?.songs?.find(it => it.id === queuedSong?.songId);
    const duration = song?.duration / 1000

    const [audio, setAudio] = useState<HTMLAudioElement>()
    useEffect(() => {
        (async () => {
            if (!localCache)
                return
            if (!playing)
                return
            if (!song)
                return

            const audioUrl = NetworkService.baseurl + '/artists/' + artist.id + '/' + album.name + '/' + song.track + '/' + format + '/' + bitrate
            if (audio && audio.dataset.audioUrl === audioUrl) {
                return
            }

            if (audio) {
                URL.revokeObjectURL(audio.dataset.objectUrl)
                audio.pause()
            }

            const cachedSong = await localCache.songByUrl(audioUrl);
            console.log('songByUrl', cachedSong, audioUrl)

            const loadAudio = (song, data) => {
                console.log('decoding', song)
                const nextAudio = new Audio();
                const blob = new Blob([new Uint8Array(data, 0, data.byteLength)])
                nextAudio.src = nextAudio.dataset.objectUrl = URL.createObjectURL(blob)
                nextAudio.dataset.audioUrl = audioUrl
                setAudio(nextAudio)
                PlayerStore.setOffset(0)
            };

            if (!cachedSong) {
                console.log('downloading', audioUrl)
                load(audioUrl, loadAudio, localCache, artist.name, album.name, song.title)
            } else {
                const sd = await localCache.songData(cachedSong.dataId)
                loadAudio(cachedSong, sd.data)
            }
        })().catch(console.error)
    }, [localCache, playing, song?.track])


    useEffect(() => {
        if (!audio)
            return

        if (playing) {
            if (audio) {
                audio.pause()
            }
            (async () => {
                console.log('start audio', offset)
                audio.currentTime = offset
                // audio.volume = 0.05
                try {
                    await audio.play()
                } catch (e) {
                    console.error(e)
                    PlayerStore.setPlaying(false)
                    return;
                }
                const startTime = performance.now() / 1000;
                clearInterval(updater)
                const interval = setInterval(() => {
                    const position = performance.now() / 1000 - startTime + offset
                    const progress = position / duration * 100
                    if (!WindowActiveStore.get().hidden)
                        setProgress(Math.min(progress, 100))
                    if (progress > 100) {
                        audio.pause()
                        clearInterval(interval)
                        PlayerStore.next()
                    }
                }, 1000 / 30);
                setUpdater(interval)
            })()
        } else {
            clearInterval(updater)
            PlayerStore.setOffset(0)
            if (!audio)
                return

            let offset = duration / 100 * progress;
            PlayerStore.setOffset(offset)

            audio.pause()
        }

    }, [audio, playing, offset])

    return <div className={css`
      position: fixed;
      bottom: 0px;
      left: 0px;
      right: 0px;
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
                    <ProgressBar progress={progress} onClick={progress => {
                        if (!audio)
                            return

                        let offset = duration / 100 * progress;
                        PlayerStore.setOffset(offset)
                        PlayerStore.setPlaying(true)
                    }}/>
                    <span className={css`margin-left: 10px;`}>
                    {audio && formatDuration(duration * 1000)}
                </span>
                </FlexRow>

                <FlexRow className={css`
                  justify-content: center;
                  margin-top: 10px;
                `}>
                    <Button className={classNames('red', css`
                      padding: 10px !important;
                      height: unset;

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
                </FlexRow>
            </div>
        </div>
    </div>
}

export default Player;