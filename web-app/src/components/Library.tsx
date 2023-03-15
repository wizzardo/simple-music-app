import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import {css, styled} from "goober";
import NetworkService, {AlbumDto, AlbumDtoSong, ArtistDto} from "../services/NetworkService";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import React, {useEffect, useRef, useState} from "react";
import {formatDuration, getAlbumDuration} from "../utils/Helpers";
import {pushLocation, replaceLocation} from "react-ui-basics/router/HistoryTools";
import Route from "react-ui-basics/router/Route";
import {FlexColumn, FlexRow} from "./SharedComponents";
import {classNames, Comparators} from "react-ui-basics/Tools";
import {SORT_ASC} from "react-ui-basics/Table";
import * as PlayerStore from "../stores/PlayerStore";
import Button from "react-ui-basics/Button";
import Scrollable, {SCROLLBAR_MODE_VISIBLE} from "react-ui-basics/Scrollable";
import {Song as SongDTO, useLocalCache} from "../services/LocalCacheService";
import * as SettingsStore from "../stores/SettingsStore";
import * as DownloadQueueStore from "../stores/DownloadQueueStore";
import {DownloadTask} from "../stores/DownloadQueueStore";
import {useWindowSize} from "../utils/Hooks";
import NavLink from "react-ui-basics/router/NavLink";


const Cover = styled("img")`
  border-radius: 4px;
  max-width: 100%;
  max-height: 150px;
`;
const SmallCover = styled("img")`
  max-width: 75px;
  max-height: 75px;
  min-width: 75px;
  min-height: 75px;
`;
const Album = styled("div")`
  display: inline-flex;
  flex-flow: column nowrap;
  padding: 5px;
  align-items: center;
  width: 200px;
  box-sizing: border-box;

  &:hover {
    cursor: pointer;
  }
`;
const AlbumTitle = styled("span")`
  margin: 5px;
  font-size: 16px;
  text-align: center;
`;
const AlbumDuration = styled("span")`
  font-size: 12px;
`;
const AlbumArtist = styled("span")`
  font-size: 12px;
  text-align: center;

  &:nth-child(2) {
    margin-top: 5px;
  }
`;

const LinkStyles = css`
  margin-right: 20px;
  font-weight: bold;
  text-transform: uppercase;
  text-decoration: none;
  color: gray;
`
const LinkActiveStyles = css`
  color: #038acc;
`

const Library = ({artistId, album}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const {queue} = useStore(PlayerStore.store)
    const albums = artistsStore.ids.map(id => artistsStore.map[id].albums.map(it => ({...it, artistId: id}))).flat()

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])


    const windowSize = useWindowSize();

    const isMobile = windowSize.width <= 800;
    let albumCardWidth = '200px'
    if (isMobile) {
        albumCardWidth = `${(windowSize.width) / 2 - 10}px`
    }

    return <Scrollable className={css`
      max-width: 100%;
      padding-right: 0;
      max-height: ${queue.length ? windowSize.height - 151 : windowSize.height}px !important;

      > .viewport {
        text-align: center;
        padding-top: 19px;
      }
    `}>
        <FlexRow className={css`
          margin-left: 50px;
          margin-bottom: 20px;
        `}>
            <NavLink className={LinkStyles} activeClassName={LinkActiveStyles} href={'/'} highlightPath={'/'}>Artists</NavLink>
            <NavLink className={LinkStyles} activeClassName={LinkActiveStyles} href={'/albums/'} highlightPath={'/albums/*'}>Albums</NavLink>
        </FlexRow>

        <Route path={"/"}>
            <ListArtists cardWidth={albumCardWidth}/>
        </Route>
        <Route path={"/albums"}>
            <ListAlbums cardWidth={albumCardWidth}/>
        </Route>
        <Route path={"/:artistId(^[0-9]+$)?"}>
            <ListArtistAlbums cardWidth={albumCardWidth} artistId={null}/>
        </Route>
        <Route path={"/:artistId/:albumName"}>
            <ListSongs albumName={null} artistId={null}/>
        </Route>
    </Scrollable>
}

export default Library;

const ListAlbums = ({cardWidth}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const albums = artistsStore.ids.map(id => artistsStore.map[id].albums.map(it => ({...it, artistId: id}))).flat()

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    albums.sort(Comparators.of(it => it.name, Comparators.SORT_ASC, albums))

    return <>
        {albums.map(it => <Album className={css`
          width: ${cardWidth};
        `} onClick={e => {
            pushLocation(`/${it.artistId}/${it.name}`)
        }}>
            {it?.coverPath && <AlbumCover artistId={it.artistId} album={it}/>}
            {!it?.coverPath && <MaterialIcon className={css`
              font-size: 50px;
            `} icon={'album'}/>}
            <AlbumTitle>{it.name}</AlbumTitle>
            <AlbumArtist>{artistsStore.map[it.artistId].name}</AlbumArtist>
            <AlbumDuration>{formatDuration(getAlbumDuration(it))}</AlbumDuration>
        </Album>)}
    </>
}

const Artist = styled("div")`
  display: inline-flex;
  flex-flow: column nowrap;
  padding: 5px;
  align-items: center;
  width: 200px;
  box-sizing: border-box;

  &:hover {
    cursor: pointer;
  }
`;

const AlbumCover = ({artistId, album}) => <Cover src={NetworkService.baseurl + '/artists/' + artistId + '/' + album.id + '/' + album.coverPath} alt={album.name}/>
const SmallAlbumCover = ({artistId, album}) => <SmallCover src={NetworkService.baseurl + '/artists/' + artistId + '/' + album.id + '/' + album.coverPath} alt={album.name}/>

const ListArtists = ({cardWidth}) => {
    const artistsStore = useStore(ArtistsStore.store)

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    let ids = artistsStore.ids.filter(id => artistsStore.map[id].albums.length > 0);
    ids.sort(Comparators.of(id => artistsStore.map[id].name, Comparators.SORT_ASC, ids))
    return <FlexRow className={css`
      flex-flow: row wrap;
      justify-content: center;
      align-items: start;
    `}>
        {ids.map(id => {
            const artist = artistsStore.map[id];

            let albumsWithCovers = artist.albums.filter(it => !!it.coverPath)
            if (albumsWithCovers.length > 4) {
                albumsWithCovers.sort(Comparators.of(getAlbumDuration, Comparators.SORT_DESC, albumsWithCovers))
            }

            return <Artist className={css`
              width: ${cardWidth};
            `} onClick={e => {
                if (artist.albums.length > 1)
                    pushLocation(`/${id}/`)
                else
                    pushLocation(`/${id}/${artist.albums[0].name}`)
            }}>
                {/*{artist.albums.length===1 && }*/}
                {albumsWithCovers.length === 1 &&
                    <AlbumCover artistId={id} album={albumsWithCovers[0]}/>}
                {(albumsWithCovers.length === 2 || albumsWithCovers.length === 3) &&
                    <FlexColumn className={css`
                      border-radius: 4px;
                      overflow: hidden;
                    `}>
                        <FlexRow>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[0]}/>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[1]}/>
                        </FlexRow>
                        <FlexRow>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[1]}/>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[0]}/>
                        </FlexRow>
                    </FlexColumn>}
                {albumsWithCovers.length >= 4 &&
                    <FlexColumn className={css`
                      border-radius: 4px;
                      overflow: hidden;
                    `}>
                        <FlexRow>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[0]}/>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[1]}/>
                        </FlexRow>
                        <FlexRow>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[2]}/>
                            <SmallAlbumCover artistId={id} album={albumsWithCovers[3]}/>
                        </FlexRow>
                    </FlexColumn>}
                {albumsWithCovers.length === 0 && <MaterialIcon className={css`
                  font-size: 50px;
                `} icon={'album'}/>}
                <AlbumArtist>{artist.name}</AlbumArtist>
                <AlbumDuration>{artist.albums.length} {artist.albums.length === 1 ? 'album' : 'albums'}</AlbumDuration>
            </Artist>;
        })}
    </FlexRow>
}


const ListArtistAlbums = ({artistId, cardWidth}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const artist = artistsStore.map[artistId];
    const albums = [...(artist?.albums || [])]

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    albums.sort(Comparators.of(it => it.name, Comparators.SORT_ASC, albums))
    if (!artistId)
        return null

    return <>
        <FlexRow className={css`
          justify-content: space-between;
          padding: 0 40px;
        `}>
            <span className={css`
              font-size: 16px;
            `}>
                {artist?.name}
            </span>

            <Button className={classNames('red', css`
              padding: 10px !important;
              height: unset;
              min-width: 50px !important;

              .MaterialIcon {
                font-size: 30px;
                color: white;
              }
            `)} flat round onClick={e => PlayerStore.play(albums.flatMap(album => album.songs.map(it => ({
                artistId: artist.id,
                albumId: album.id,
                songId: it.id
            }))))}>
                <MaterialIcon icon={'play_arrow'}/>
            </Button>
        </FlexRow>
        <FlexRow className={css`
          flex-flow: row wrap;
          justify-content: center;
          align-items: start;
        `}>
            {albums.map(it => <Album className={css`
              width: ${cardWidth};
            `} onClick={e => {
                pushLocation(`/${artistId}/${it.name}`)
            }}>
                {it?.coverPath && <AlbumCover artistId={artistId} album={it}/>}
                {!it?.coverPath && <MaterialIcon className={css`
                  font-size: 50px;
                `} icon={'album'}/>}
                <AlbumTitle>{it.name}</AlbumTitle>
                <AlbumArtist>{artist.name}</AlbumArtist>
                <AlbumDuration>{formatDuration(getAlbumDuration(it))}</AlbumDuration>
            </Album>)}
        </FlexRow>
    </>
}


const ListSongs = ({artistId, albumName}) => {
    const [notReadySongs, setNotReadySongs] = useState([])
    const {format, bitrate} = useStore(SettingsStore.store)
    const cache = useLocalCache()
    const downloadQueueState = useStore(DownloadQueueStore.store)

    const artistsStore = useStore(ArtistsStore.store)
    const artist = artistsStore.map[artistId];

    albumName = decodeURIComponent(albumName)
    const album = artist?.albums?.find(it => it.name === albumName)

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    const refSeparatorSongs = useRef<HTMLSpanElement>()
    const [downloadClicked, setDownloadClicked] = useState(false)

    const songs = album ? [...album.songs] : []
    songs.sort(Comparators.of('track', SORT_ASC, songs))

    useEffect(() => {
        if (!cache)
            return
        if (!songs.length)
            return;

        cache && (async () => {
            let notReady: DownloadTask[] = []
            for (let i = 0; i < songs.length; i++) {
                const url = NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + songs[i].id + '/' + format + '/' + bitrate
                const cachedSong = await cache.songByUrl(url)
                if (!cachedSong) {
                    notReady.push({
                        url,
                        song: songs[i].title,
                        album: album.name,
                        artist: artist.name,
                        bitrate,
                        format
                    })
                }
            }
            setNotReadySongs(notReady)
        })()

    }, [cache, album?.songs, downloadQueueState.queue])

    if (!album)
        return <></>


    let isMobile = window.innerWidth <= 800;

    return <FlexRow className={css`
      margin: ${isMobile ? 0 : '20px'};
      margin-bottom: 0;
      flex-flow: row ${isMobile ? 'wrap' : 'nowrap'};
      align-items: flex-start;`}>

        {album.coverPath && <Cover
            className={classNames(isMobile && css`
              width: 100%;
              max-height: 300px;
              max-width: 300px;
              margin-left: auto;
              margin-right: auto;
              margin-bottom: 20px;
            `)}
            src={NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + album.coverPath} alt={album.name}
        />}
        {!album.coverPath && <MaterialIcon className={css`
          font-size: 50px;
        `} icon={'album'}/>}

        {!isMobile && <span className={css`width: 25px;`}/>}

        <FlexColumn className={css`
          flex-basis: 1px;
          flex-grow: 1;
          padding-left: 20px;
          padding-right: 20px;
        `}>
            <FlexRow className={css`justify-content: space-between;`}>
                <FlexColumn>
                    {album && artist && album.name}

                    <span className={css`height: 5px;`}/>

                    <span className={css`font-size: 14px;`}>
                    by {artist.name}
                </span>
                </FlexColumn>

                <Button className={classNames('red', css`
                  padding: 10px !important;
                  height: unset;
                  min-width: 50px !important;

                  .MaterialIcon {
                    font-size: 30px;
                    color: white;
                  }
                `)} flat round onClick={e => PlayerStore.play(songs.map(it => ({
                    artistId: artist.id,
                    albumId: album.id,
                    songId: it.id
                })))}>
                    <MaterialIcon icon={'play_arrow'}/>
                </Button>
            </FlexRow>

            <span className={css`height: 10px;`}/>

            <FlexRow>
                tracks: {album.songs.length}
                <span className={css`width: 25px;`}/>
                duration: {formatDuration(getAlbumDuration(album))}
                <span className={css`width: 25px;`}/>
                {notReadySongs.length > 0 && <Button
                    flat
                    round
                    disabled={downloadClicked}
                    onClick={e => {
                        setDownloadClicked(true)
                        DownloadQueueStore.downloadAll(notReadySongs)
                    }}
                >
                    <MaterialIcon icon={'download_for_offline'}/>
                </Button>}
            </FlexRow>

            <span className={css`height: 25px;`} ref={refSeparatorSongs}/>

            <Scrollable scrollBarMode={SCROLLBAR_MODE_VISIBLE} className={css`
              max-width: 100%;
              max-height: ${isMobile ? 9999 + 'px' : (refSeparatorSongs.current ? (window.innerHeight - 151 - refSeparatorSongs.current.getBoundingClientRect().bottom) + 'px' : '600px')};
            `}>
                {songs.map(it => <Song key={it.id} artist={artist} album={album} song={it}/>)}
            </Scrollable>
        </FlexColumn>


    </FlexRow>
}

const Song = ({artist, album, song}: { artist: ArtistDto, album: AlbumDto, song: AlbumDtoSong }) => {
    const {position, queue} = useStore(PlayerStore.store)
    const {format, bitrate} = useStore(SettingsStore.store)
    const downloadQueueState = useStore(DownloadQueueStore.store)

    const cache = useLocalCache()
    const [cachedSong, setCachedSong] = useState<SongDTO>()
    const [inDownloadQueue, setInDownloadQueue] = useState(false)
    const [downloadRequested, setDownloadRequested] = useState(false)

    useEffect(() => {
        cache && (async () => {
            const audioUrl = NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + song.id + '/' + format + '/' + bitrate
            const cachedSong = await cache.songByUrl(audioUrl)
            setCachedSong(cachedSong)
        })()
    }, [cache, inDownloadQueue])

    useEffect(() => {
        if (cachedSong)
            return

        const queued = !!downloadQueueState.queue.find(it => it.artist === artist.name && it.album === album.name && it.song === song.title);
        setInDownloadQueue(queued)
        queued && setDownloadRequested(queued)
    }, [cachedSong, downloadQueueState.queue])

    const isCurrentSong = queue[position]?.songId === song.id
    return <FlexRow className={css`
      padding: 5px;

      .MaterialIcon {
        font-size: 16px;
        color: gray;
      }
    `}>
        <FlexRow
            className={css`
              flex-grow: 1;
              margin-right: 20px;
              cursor: pointer;
            `}
            onClick={e => {
                PlayerStore.play(album.songs.map(it => ({
                    artistId: artist.id,
                    albumId: album.id,
                    songId: it.id
                })), album.songs.findIndex(it => it.id === song.id))
            }}
        >
            <span className={css`width: 20px;
              text-align: right;
              margin-right: 10px;`}>{song.track}.</span>

            <span className={css`
              display: inline-flex;
              flex-basis: 1px;
              flex-grow: 1;
              color: ${isCurrentSong ? '#c02727' : 'black'};
              font-weight: ${isCurrentSong ? 'bold' : 'normal'};
            `}>{song.title}</span>
        </FlexRow>

        {!!cachedSong && downloadRequested && <span className={css`
          display: inline-flex;
          flex-flow: row;
          align-items: center;
          justify-content: center;
          width: 24px;
          height: 24px;
        `}>
            <MaterialIcon icon={'download_done'}/>
        </span>}
        {!cachedSong && <Button
            flat
            round
            disabled={inDownloadQueue}
            className={css`
              min-width: 24px !important;
              min-height: 24px;
            `}
            onClick={e => {
                DownloadQueueStore.download(
                    NetworkService.baseurl + '/artists/' + artist.id + '/' + album.id + '/' + song.id + '/' + format + '/' + bitrate,
                    artist.name,
                    album.name,
                    song.title,
                    format,
                    bitrate
                )
            }}
        >
            <MaterialIcon icon={'download_for_offline'}/>
        </Button>}

    </FlexRow>
}