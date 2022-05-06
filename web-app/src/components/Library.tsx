import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import {css, styled} from "goober";
import NetworkService, {AlbumDto, AlbumDtoSong, ArtistDto} from "../services/NetworkService";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import React, {useEffect, useRef} from "react";
import {formatDuration, getAlbumDuration} from "../utils/Helpers";
import {pushLocation, replaceLocation} from "react-ui-basics/router/HistoryTools";
import Route from "react-ui-basics/router/Route";
import {FlexColumn, FlexRow} from "./SharedComponents";
import {classNames, Comparators} from "react-ui-basics/Tools";
import {SORT_ASC} from "react-ui-basics/Table";
import * as PlayerStore from "../stores/PlayerStore";
import Button from "react-ui-basics/Button";
import Scrollable, {SCROLLBAR_MODE_VISIBLE} from "react-ui-basics/Scrollable";


const Cover = styled("img")`
  border-radius: 4px;
  max-width: 150px;
  max-height: 150px;
`;
const Album = styled("div")`
  display: inline-flex;
  flex-flow: column nowrap;
  padding: 5px;
  align-items: center;

  &:hover {
    cursor: pointer;
  }
`;
const AlbumTitle = styled("span")`
  margin: 5px;
  font-size: 16px;
`;
const AlbumDuration = styled("span")`
  color: gray;
  font-size: 12px;
`;
const AlbumArtist = styled("span")`
  color: gray;
  font-size: 12px;
`;

const LibraryDiv = styled("div")`
`;

const Library = ({artistId, album}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const albums = artistsStore.ids.map(id => artistsStore.map[id].albums.map(it => ({...it, artistId: id}))).flat()

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    return <LibraryDiv>
        <Route path={"/"}>
            <ListArtists/>
        </Route>
        <Route path={"/:artistId"}>
            <ListAlbums artistId={null}/>
        </Route>
        <Route path={"/:artistId/:albumName"}>
            <ListSongs albumName={null} artistId={null}/>
        </Route>
    </LibraryDiv>
}

export default Library;

const ListArtists = () => {
    const artistsStore = useStore(ArtistsStore.store)
    const albums = artistsStore.ids.map(id => artistsStore.map[id].albums.map(it => ({...it, artistId: id}))).flat()

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    return <>
        {albums.map(it => <Album onClick={e => {
            pushLocation(`/${it.artistId}/${it.name}`)
        }}>
            {it?.coverPath && <Cover src={NetworkService.baseurl + '/artists/' + artistsStore.map[it.artistId].path + '/' + it.path + '/' + it.coverPath} alt={it.name}/>}
            {!it?.coverPath && <MaterialIcon className={css`
              font-size: 50px;
            `} icon={'album'}/>}
            <AlbumTitle>{it.name}</AlbumTitle>
            <AlbumArtist>{artistsStore.map[it.artistId].name}</AlbumArtist>
            <AlbumDuration>{formatDuration(getAlbumDuration(it))}</AlbumDuration>
        </Album>)}
    </>
}


const ListAlbums = ({artistId}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const artist = artistsStore.map[artistId];
    const albums = artist?.albums || []

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    return <>
        {albums.map(it => <Album onClick={e => {
            pushLocation(`/${artistId}/${it.name}`)
        }}>
            {it?.coverPath && <Cover src={NetworkService.baseurl + '/artists/' + artist.path + '/' + it.path + '/' + it.coverPath} alt={it.name}/>}
            {!it?.coverPath && <MaterialIcon className={css`
              font-size: 50px;
            `} icon={'album'}/>}
            <AlbumTitle>{it.name}</AlbumTitle>
            <AlbumArtist>{artist.name}</AlbumArtist>
            <AlbumDuration>{formatDuration(getAlbumDuration(it))}</AlbumDuration>
        </Album>)}
    </>
}


const ListSongs = ({artistId, albumName}) => {
    const artistsStore = useStore(ArtistsStore.store)
    const artist = artistsStore.map[artistId];

    albumName = decodeURIComponent(albumName)
    const album = artist?.albums?.find(it => it.name === albumName)

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    if (!album)
        return <></>

    const songs = [...album.songs]
    songs.sort(Comparators.of('track', SORT_ASC, songs))

    const refSeparatorSongs = useRef<HTMLSpanElement>()

    return <FlexRow className={css`margin: 20px;
      align-items: flex-start;`}>
        {album.coverPath && <Cover src={NetworkService.baseurl + '/artists/' + artist.path + '/' + album.path + '/' + album.coverPath} alt={album.name}/>}
        {!album.coverPath && <MaterialIcon className={css`
          font-size: 50px;
        `} icon={'album'}/>}

        <span className={css`width: 25px;`}/>

        <FlexColumn className={css`
          flex-basis: 1px;
          flex-grow: 1;
        `}>
            {album && artist && album.name}

            <span className={css`height: 5px;`}/>

            <span className={css`
              font-size: 14px;`}>
                by {artist.name}
            </span>

            <span className={css`height: 10px;`}/>

            <FlexRow>
                tracks: {album.songs.length}
                <span className={css`width: 25px;`}/>
                duration: {formatDuration(getAlbumDuration(album))}
            </FlexRow>

            <span className={css`height: 25px;`} ref={refSeparatorSongs}/>

            <Scrollable scrollBarMode={SCROLLBAR_MODE_VISIBLE} className={css`
              max-height: ${refSeparatorSongs.current ? (window.innerHeight - 187 - refSeparatorSongs.current.getBoundingClientRect().bottom) + 'px' : '600px'};            
`}>
                {songs.map(it => <Song key={it.id} artist={artist} album={album} song={it}/>)}
            </Scrollable>
        </FlexColumn>

        <Button className={classNames('red', css`
          padding: 10px !important;
          height: unset;

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
}

const Song = ({artist, album, song}: { artist: ArtistDto, album: AlbumDto, song: AlbumDtoSong }) => {
    const {position, queue} = useStore(PlayerStore.store)
    const isCurrentSong = queue[position]?.songId === song.id
    return <FlexRow className={css`padding: 2px;`}>
        <span className={css`width: 50px;`}>{song.track}.</span>
        <span className={css`
          color: ${isCurrentSong ? 'red' : 'black'};
        `}>{song.title}</span>
    </FlexRow>
}