import React, {useEffect, useState} from 'react';
import './App.css'
import {classNames} from "react-ui-basics/Tools";
import Button from "react-ui-basics/Button";
import ProgressBar from "./ProgressBar";
import {SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";

const audioUrl = "https://cdn.pixabay.com/download/audio/2022/03/23/audio_07b2a04be3.mp3?filename=order-99518.mp3";
const audioUrl2 = "https://cdn.pixabay.com/download/audio/2022/01/26/audio_d0c6ff1bdd.mp3?filename=the-cradle-of-your-soul-15700.mp3";

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

const playSound = (context, buffer, offset) => {
    var source = context.createBufferSource();
    source.buffer = buffer;
    source.connect(context.destination);
    source.start(0, offset);
    return source
};

export default () => {
    const [context, setContext] = useState()
    const localCache = useLocalCache();

    useEffect(() => {
        window.AudioContext = window.AudioContext || window['webkitAudioContext'];
        setContext(new AudioContext())
    }, [])


    const [audio, setAudio] = useState()
    useEffect(async () => {
        if (!context)
            return
        if (!localCache)
            return

        const song = await localCache.songByUrl(audioUrl);
        console.log('songByUrl', song)
        if (!song) {
            load(audioUrl, context, setAudio, localCache)
        } else {
            const sd = await localCache.songData(song.dataId)
            context.decodeAudioData(sd.data, setAudio, e => console.error(e))
        }
    }, [context, localCache])

    const [playing, setPlaying] = useState(false)
    const [source, setSource] = useState(false)
    const [progress, setProgress] = useState(0)
    const [offset, setOffset] = useState(0)
    const [updater, setUpdater] = useState()

    useEffect(() => {
        if (!source)
            return

        if (playing) {
            clearInterval(updater)
            source.stop()
        }
    }, [offset])

    useEffect(() => {
        if (!context)
            return
        if (!audio)
            return

        if (playing) {
            setSource(playSound(context, audio, offset))
            const startTime = context.currentTime;
            setUpdater(setInterval(() => {
                const duration = audio.duration
                const position = context.currentTime - startTime + offset
                const progress = position / duration * 100
                setProgress(Math.min(progress, 100))
                if (progress > 100) {
                    clearInterval(updater)
                    setOffset(0)
                }
            }, 1000 / 30))
        } else {
            clearInterval(updater)
            setOffset(0)
            if (!source)
                return

            source.stop()
        }

    }, [context, audio, playing, offset])


    return (
        <div className={classNames("App")}>
            {/*{audio?.duration}*/}

            <br/>

            <ProgressBar progress={progress} onClick={progress => {
                if (!context)
                    return
                if (!audio)
                    return

                let offset = audio.duration / 100 * progress;
                setOffset(offset)
                setPlaying(true)
            }}/>

            <br/>

            <Button onClick={e => setPlaying(!playing)}>{!playing ? 'play' : 'stop'}</Button>
        </div>
    );
}

