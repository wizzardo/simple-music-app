import {Song, useLocalCache} from "../services/LocalCacheService";
import {useEffect, useState} from "react";

const CacheStats = ({}) => {
    const localCache = useLocalCache();

    const [songs, setSongs] = useState<Song[]>([])

    useEffect(() => {
        localCache && (async () => {
            const songs = await localCache.songs();
            setSongs(songs)
        })()
    }, [localCache])
    return <div>
        songs:
        {songs.map((it, i) => <div key={i}>{it.size}</div>)}
    </div>
}

export default CacheStats