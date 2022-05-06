import ProgressBar from "./ProgressBar";
import Button from "react-ui-basics/Button";
import React, {useEffect, useRef, useState} from "react";

import {SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";
import {css} from "goober";
import {formatDuration} from "../utils/Helpers";
import {FlexRow} from "./SharedComponents";
import MaterialIcon from "../../../../react-ui-basics/build/MaterialIcon";
import {classNames} from "../../../../react-ui-basics/build/Tools";
import {useStore} from "../../../../react-ui-basics/build/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import * as PlayerStore from "../stores/PlayerStore";
import NetworkService from "../services/NetworkService";
import {setOffset} from "../stores/PlayerStore";
import WindowActiveStore from "../stores/WindowActiveStore";


window.AudioContext = window.AudioContext || window['webkitAudioContext'];
const context = new AudioContext()

// const a = new Audio('');

const load = (url, context, setAudio, localCache: SongLocalCacheDB) => {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'arraybuffer';

    request.onload = () => {
        let audioData = request.response;
        const data = audioData.slice();
        context.decodeAudioData(audioData, buffer => {
            localCache.add({
                url,
                album: 'test',
                artist: 'test',
                size: data.byteLength,
                dataId: 0,
            }, data)
            setAudio(buffer)
        }, e => console.error(e));
    }
    request.send();
};

const playSound = (context: AudioContext, buffer, offset): AudioBufferSourceNode => {
    var source = context.createBufferSource();
    source.buffer = buffer;
    source.connect(context.destination);
    source.start(0, offset);
    return source
};

let prev = [];

const Player = ({}) => {
    const localCache = useLocalCache();

    const artistsStore = useStore(ArtistsStore.store)
    const {playing, position, queue, offset} = useStore(PlayerStore.store)

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
    const song = album?.songs?.find(it => it.id === queuedSong?.songId);


    const [audio, setAudio] = useState<AudioBuffer>()
    useEffect(() => {
        (async () => {
            if (!localCache)
                return
            if (!playing)
                return
            if (!song)
                return

            console.log('useEffect called', prev[0] !== localCache, prev[1] !== playing, prev[2] !== song?.track)
            prev = [localCache, playing, song?.track];

            const audioUrl = NetworkService.baseurl + '/artists/' + artist.id + '/' + album.name + '/' + song.track

            const cachedSong = await localCache.songByUrl(audioUrl);
            console.log('songByUrl', cachedSong, audioUrl)
            if (!cachedSong) {
                console.log('downloading', audioUrl)
                load(audioUrl, context, setAudio, localCache)
            } else {
                const sd = await localCache.songData(cachedSong.dataId)
                console.log('decoding', audioUrl)
                const audioBuffer = await context.decodeAudioData(sd.data)
                setAudio(audioBuffer)
                setOffset(0)
            }
        })().catch(console.error)
    }, [localCache, playing, song?.track])

    const [source, setSource] = useState<AudioBufferSourceNode>()
    const [progress, setProgress] = useState(0)
    const [updater, setUpdater] = useState<NodeJS.Timer>()


    useEffect(() => {
        if (!audio)
            return

        if (playing) {
            if (source) {
                source.stop()
            }

            console.log('start audio', audio, offset)
            let sourceNode = playSound(context, audio, offset);
            sourceNode.addEventListener('ended', ev => {
                sourceNode.disconnect()
            })
            setSource(sourceNode)
            const startTime = context.currentTime;
            clearInterval(updater)
            const interval = setInterval(() => {
                const duration = audio.duration
                const position = context.currentTime - startTime + offset
                const progress = position / duration * 100
                if (!WindowActiveStore.get().hidden)
                    setProgress(Math.min(progress, 100))
                if (progress > 100) {
                    sourceNode.stop()
                    clearInterval(interval)
                    PlayerStore.next()
                }
            }, 1000 / 30);
            setUpdater(interval)
        } else {
            clearInterval(updater)
            PlayerStore.setOffset(0)
            if (!source)
                return

            let offset = audio.duration / 100 * progress;
            PlayerStore.setOffset(offset)

            source.stop()
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
              width: 600px;
              margin-left: auto;
              margin-right: auto;
            `}>
                <FlexRow>
                <span className={css`margin-right: 10px;`}>
                    {audio && formatDuration(audio.duration * 1000 / 100 * progress)}
                </span>
                    <ProgressBar progress={progress} onClick={progress => {
                        if (!context)
                            return
                        if (!audio)
                            return

                        let offset = audio.duration / 100 * progress;
                        PlayerStore.setOffset(offset)
                        PlayerStore.setPlaying(true)
                    }}/>
                    <span className={css`margin-left: 10px;`}>
                    {audio && formatDuration(audio.duration * 1000)}
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
                    `)} flat round onClick={e => queue.length && PlayerStore.setPlaying(!playing)}>
                        <MaterialIcon icon={!playing ? 'play_arrow' : 'pause'}/>
                    </Button>
                </FlexRow>
            </div>
        </div>
    </div>
}

export default Player;