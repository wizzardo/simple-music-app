import {Song, useLocalCache} from "../services/LocalCacheService";
import React, {useEffect, useState} from "react";
import Table, {TableColumnTyped} from "react-ui-basics/Table";
import Size from "react-ui-basics/Size";
import Button from "react-ui-basics/Button";
import {Comparators} from "react-ui-basics/Tools";
import {css} from "goober";

interface CachedAlbum {
    name: string,
    artist: string,
    songs: Song[],
}

const CacheStats = ({}) => {
    const localCache = useLocalCache();

    const [refreshed, setRefreshed] = useState(new Date().getTime())
    const [songs, setSongs] = useState<Song[]>([])
    const [albums, setAlbums] = useState<CachedAlbum[]>([])

    useEffect(() => {
        localCache && (async () => {
            const songs = await localCache.songs();
            setSongs(songs)
            console.log(songs)
            const albums = {}
            songs.forEach(it => {
                let albumKey = it.artist + ' - ' + it.album
                if (!albums[albumKey])
                    albums[albumKey] = {
                        name: it.album,
                        artist: it.artist,
                        songs: []
                    }

                const album = albums[albumKey]
                album.songs.push(it)
            })
            setAlbums(Object.values(albums))
        })()
    }, [localCache, refreshed])
    return <div className={css`
      padding: 60px 20px 20px;
    `}>
        <Table<CachedAlbum>
            sortBy={'name'}
            data={albums}
            columns={[
                {
                    field: 'name',
                    header: 'Name',
                    sortable: true,
                },
                {
                    field: 'songs',
                    header: 'Tracks',
                    sortable: false,
                    formatter: ((it) => String(it.length))
                },
                {
                    field: 'songs',
                    header: 'Size',
                    sortable: true,
                    formatter: ((it) => <Size value={it.reduce((total, it) => {
                        total += it.size;
                        return total
                    }, 0)}/>),
                    comparator: Comparators.of(it => it.songs.reduce((total, it) => {
                        total += it.size;
                        return total
                    }, 0), Comparators.SORT_ASC, albums)
                } as TableColumnTyped<CachedAlbum, 'songs'>,
                {
                    field: 'name',
                    header: '',
                    sortable: false,
                    formatter: ((name, it) => <Button className={'red'} onClick={async () => {
                        for (let i = 0; i < it.songs.length; i++) {
                            await localCache.deleteSong(it.songs[i])
                        }
                        setRefreshed(new Date().getTime())
                    }}>delete</Button>)
                },
            ]}/>


    </div>
}

export default CacheStats