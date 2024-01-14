import {styled} from "goober";
import {css} from "goober";
import Route from "react-ui-basics/router/Route";
import Button from "react-ui-basics/Button";
import TextField from "react-ui-basics/TextField";
import AutocompleteSelect, {MODE_MINI} from "react-ui-basics/AutocompleteSelect";
import Dropzone from "react-ui-basics/Dropzone";
import Checkbox from "react-ui-basics/Checkbox";
import Link from "react-ui-basics/router/Link";
import NavLink from "react-ui-basics/router/NavLink";
import {pushLocation, replaceLocation} from "react-ui-basics/router/HistoryTools";
import NetworkService, {AlbumDto, AlbumDtoSong, ArtistDto} from "../services/NetworkService";
import React, {useEffect, useState} from "react";
import Table, {TableColumnTyped} from "react-ui-basics/Table";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import dayjs from "dayjs";
import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import {classNames, Comparators} from "react-ui-basics/Tools";
import * as DialogStore from "../stores/DialogStore";
import SpinningProgress from "react-ui-basics/SpinningProgress";
import {UploadForm} from "./UploadForm";
import {formatDuration, getAlbumDuration} from "../utils/Helpers";
import {FlexColumn, FlexRow, smallIconButtonCss} from "./SharedComponents";
import {createProxy} from 'react-ui-basics/store/ProxyTools.js';
import AlbumCover from "./AlbumCover";

export const DateFormatter = (date, item, format = 'YY-MM-DD hh:mm:ss') => date && dayjs(date).format(format);

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

export interface LibraryEditorProps {
    artistId?: number
    album?: string
}

const LibraryEditor = ({artistId, album}: LibraryEditorProps) => {
    const artistsStore = useStore(ArtistsStore.store)

    const artists = artistsStore.ids.map(id => artistsStore.map[id])
    const artist = artistsStore.map[artistId]

    useEffect(() => {
        NetworkService.getArtists().then(ArtistsStore.setAll)
    }, [])

    useEffect(() => {
        artistId && NetworkService.getArtist({id: artistId}).then(ArtistsStore.set)
    }, [artistId])

    let albumName = album && decodeURI(album);
    let albumDto: AlbumDto = artistsStore.map[artistId]?.albums?.find(it => it.name === albumName);
    return <div className={css`
      background: white;
      padding: 20px;
      padding-bottom: 150px;
    `}>
        <FlexRow className={css`
          margin-left: 50px;
          margin-bottom: 20px;
        `}>
            <NavLink className={LinkStyles} activeClassName={LinkActiveStyles} href={'/edit/artists/'} highlightPath={'/edit/artists/*'}>Artists</NavLink>
            <NavLink className={LinkStyles} activeClassName={LinkActiveStyles} href={'/edit/albums/'} highlightPath={'/edit/albums/*'}>Albums</NavLink>
        </FlexRow>

        {artist && <Link href={'/edit/artists/'}>Artists:</Link>}
        &nbsp;&nbsp;
        {artist && <Link href={'/edit/artists/' + artistId + '/'}>{artist.name}:</Link>}
        &nbsp;&nbsp;
        {albumDto && <span>{albumDto.name}:</span>}
        <br/>
        <br/>

        <>
            <Route path={"/edit/artists"}>
                <ListArtists artists={artists}/>
            </Route>
            <Route path={"/edit/albums"}>
                <ListAlbumsArtists artists={artists}/>
            </Route>
            <Route path={"/edit/artists/:artistId"}>
                {artist && <ListAlbums artist={artist}/>}
            </Route>
            <Route path={"/edit/artists/:artistId/:albumName"}>
                {artist && <ListSongs album={albumDto} artist={artist}/>}
            </Route>
        </>

        <UploadForm artistId={artistId} albumId={albumDto?.id}/>
    </div>
}

export default LibraryEditor

const ListArtists = ({artists}) => {
    return <>
        <Table sortBy={'name'}
               data={artists}
               onRowClick={(it, e) => {
                   // @ts-ignore
                   if (e.target?.closest('.Button'))
                       return

                   pushLocation('/edit/artists/' + it['id'] + '/')
               }}
               rowClassName={css`
                 &:hover {
                   cursor: pointer;
                 }
               `}
               columns={[
                   {
                       field: 'name',
                       header: 'Name',
                       sortable: true,
                   },
                   {
                       field: 'updated',
                       header: 'Date updated',
                       sortable: true,
                       formatter: DateFormatter
                   },
                   {
                       field: 'albums',
                       header: 'Albums',
                       sortable: true,
                       formatter: (albums) => `${albums.length}`
                   },
                   {
                       field: 'id',
                       header: '',
                       sortable: false,
                       formatter: ((id, item) => <Button round flat className={css`color: grey;`} onClick={e => {
                           DialogStore.show({
                               title: 'Are you sure to remove artist?',
                               description: <>
                                   <b>{item.name}</b>
                                   <br/><br/>
                                   This action cannot be undone!
                               </>,
                               buttons: <>
                                   <Button className={'red'} onClick={() => {
                                       NetworkService.deleteArtist({id}).then(() => {
                                           NetworkService.getArtists().then(ArtistsStore.setAll)
                                           DialogStore.hide()
                                       })
                                   }}>delete</Button>
                               </>,
                           })
                       }}>
                           <MaterialIcon icon={'delete'}/>
                       </Button>)
                   }
               ]}/>

        <FlexRow className={css`
          justify-content: flex-end;
          margin-top: 20px;
        `}>
            <Button className={css`margin-right: 20px;`} onClick={e => {
                const Form = ({}) => {
                    const [name, setName] = useState('')

                    const onCreateClick = e => {
                        NetworkService.createArtist({name}).then((artist) => {
                            ArtistsStore.set(artist)
                            DialogStore.hide()
                        })
                    }

                    return <>
                        <TextField value={name} onChange={e => setName(e.target.value)}/>
                        <FlexRow className={css`
                          justify-content: flex-end;
                          margin-top: 20px;
                        `}>
                            <Button onClick={onCreateClick}>Create</Button>
                        </FlexRow>
                    </>
                }

                DialogStore.show({
                    title: 'Create new artist',
                    description: <Form/>,
                })
            }}>
                Create
            </Button>
        </FlexRow>
    </>
}

interface Album extends AlbumDto {
    artistId: number
}


const ListAlbumsArtists = ({artists}: { artists: ArtistDto[] }) => {
    const artistsStore = useStore(ArtistsStore.store)
    const albums = artists.map(artist => artist.albums.map(it => ({...it, artistId: artist.id}))).flat()

    const [selected, setSelected] = useState<string[]>([])
    const [merging, setMerging] = useState(false)

    const moveAlbum = async (album, toArtistId) => {
        await NetworkService.moveAlbum({artistId: album.artistId, albumId: album.id, toArtistId})
    }

    const mergeAlbums = async () => {
        setMerging(true)
        const ids = [...selected]
        ids.sort(Comparators.of(id => getAlbumDuration(albums.find(it => it.id === id)), Comparators.SORT_ASC, selected))
        const intoAlbumId = ids.pop()

        const intoAlbum = albums.find(it => it.id === intoAlbumId)
        const albumsToMove = ids.map(id => albums.find(it => it.id === id))
        for (let i = 0; i < albumsToMove.length; i++) {
            await moveAlbum(albumsToMove[i], intoAlbum.artistId)
        }

        await NetworkService.mergeAlbums({artistId: intoAlbum.artistId, intoAlbumId, albums: ids})
        await NetworkService.getArtists().then(ArtistsStore.setAll)
        setMerging(false)
        setSelected([])
    }


    return <>
        <Table<Album>
            sortBy={'name'}
            data={albums}
            onRowClick={(it, e) => {
                // @ts-ignore
                if (e.target?.closest('.Button'))
                    return
                // @ts-ignore
                if (e.target?.closest('.Checkbox'))
                    return

                pushLocation('/edit/artists/' + it.artistId + '/' + it.path)
            }}
            rowClassName={css`
              &:hover {
                cursor: pointer;
              }
            `}
            columns={[
                {
                    field: 'coverPath',
                    sortable: false,
                    formatter: ((it, item) => <AlbumCover artistId={item.artistId} album={item} forceShow={true}/>)
                },
                {
                    field: 'name',
                    header: 'Name',
                    sortable: true,
                },
                {
                    field: 'artistId',
                    header: 'Artist',
                    sortable: true,
                    formatter: (id) => artistsStore.map[id]?.name,
                    comparator: Comparators.of((it) => artistsStore.map[it.artistId]?.name, Comparators.SORT_ASC, albums)
                } as TableColumnTyped<Album, 'artistId'>,
                {
                    field: 'date',
                    header: 'Date',
                    sortable: true,
                },
                {
                    field: 'songs',
                    header: 'Tracks',
                    sortable: false,
                    formatter: ((it) => String(it.length))
                } as TableColumnTyped<Album, 'songs'>,
                {
                    field: 'songs',
                    header: 'Duration',
                    sortable: false,
                    formatter: ((it) => formatDuration(it.reduce((total, it) => {
                        total += it.duration;
                        return total
                    }, 0) || 0))
                } as TableColumnTyped<Album, 'songs'>,
                {
                    field: 'id',
                    header: '',
                    sortable: false,
                    formatter: ((id) => <Checkbox value={selected.includes(id)} onChange={e => {
                        if (e.target.checked) {
                            setSelected([...selected, id])
                        } else {
                            setSelected(selected.filter(it => it !== id))
                        }
                    }}/>)
                } as TableColumnTyped<Album, 'id'>,
                {
                    field: 'id',
                    sortable: false,
                    formatter: ((id, item) => <Button round flat className={css`color: grey;`} onClick={e => {
                        DialogStore.show({
                            title: 'Are you sure to remove album?',
                            description: <>
                                <b>{item.name}</b>
                                <br/><br/>
                                This action cannot be undone!
                            </>,
                            buttons: <>
                                <Button className={'red'} onClick={() => {
                                    NetworkService.deleteAlbum({artistId: item.artistId, albumId: item.id}).then(() => {
                                        NetworkService.getArtist({id: item.artistId}).then(ArtistsStore.set)
                                        DialogStore.hide()
                                    })
                                }}>delete</Button>
                            </>,
                        })
                    }}>
                        <MaterialIcon icon={'delete'}/>
                    </Button>)
                }
            ]}/>

        <FlexRow className={css`
          justify-content: flex-end;
          margin-top: 20px;
        `}>
            <Button className={css`margin-right: 20px;`} onClick={e => {
                const Form = ({}) => {
                    const [name, setName] = useState('')

                    const onCreateClick = e => {
                        NetworkService.createArtist({name}).then((artist) => {
                            ArtistsStore.set(artist)
                            DialogStore.hide()
                        })
                    }

                    return <>
                        <TextField value={name} onChange={e => setName(e.target.value)}/>
                        <FlexRow className={css`
                          justify-content: flex-end;
                          margin-top: 20px;
                        `}>
                            <Button onClick={onCreateClick}>Create</Button>
                        </FlexRow>
                    </>
                }

                DialogStore.show({
                    title: 'Create new artist',
                    description: <Form/>,
                })
            }}>
                Create
            </Button>

            <Button disabled={merging || selected.length < 2} onClick={mergeAlbums}>
                {merging && <SmallSpinner/>}
                Merge
            </Button>
        </FlexRow>
    </>
}


const Cover = styled("img")`
  border-radius: 4px;
  max-width: 150px;
  max-height: 150px;
`;
const CoverSmall = styled(Cover)`
  max-width: 50px;
  max-height: 50px;
`;


const SmallSpinner = () => <span className={css`
  .SpinningProgress {
    width: 24px;
    height: 24px;
    margin-left: 10px;
    margin-right: 10px;

    .line {
      stroke: #039be5;
    }
  }
`}>
    <SpinningProgress/>
</span>

const EditableTitle = ({value, onSave}: { value: string, onSave: (newValues: string) => Promise<any> }) => {
    const [editing, setEditing] = useState(false)
    const [inputValue, setInputValue] = useState<string>()
    const [shouldFocus, setShouldFocus] = useState(true)
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        setInputValue(value || '')
    }, [value])

    return <FlexRow>
        {!editing && <b>{inputValue}</b>}
        {editing && <TextField focused={shouldFocus}
                               onKeyDown={e => {
                                   // console.log(e.keyCode, e)
                                   if (e.keyCode === 13 /*enter*/) {
                                       setEditing(false)
                                       setShouldFocus(true)
                                       setSaving(true)
                                       const promise = onSave(inputValue);
                                       if (!promise)
                                           setSaving(false)
                                       else
                                           promise.then(() => setSaving(false))
                                   }
                               }}
                               onChange={e => {
                                   setInputValue(e.target.value)
                               }}
                               onFocus={() => {
                                   setShouldFocus(false)
                               }}
                               onBlur={e => {
                                   if (inputValue === value) {
                                       setEditing(false)
                                       setShouldFocus(true)
                                   }
                               }}
                               value={inputValue}/>
        }

        {!saving && <Button flat={true} round={true} className={classNames(smallIconButtonCss, css`margin-left: 10px;`)} onClick={e => {
            setEditing(!editing);
            if (editing) {
                setShouldFocus(true)
                onSave(inputValue)
            }
        }}>
            {!editing && <MaterialIcon icon={'edit'}/>}
            {editing && <MaterialIcon icon={'done'}/>}
        </Button>}

        {saving && <SmallSpinner/>}
    </FlexRow>
}

const EditableTitleSelect = ({value, onSave, ids, labels, label, children}: { value: any, ids: any[], label: any, labels: any, children: any, onSave: (any) => Promise<any> }) => {
    const [editing, setEditing] = useState(false)
    const [inputValue, setInputValue] = useState()
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        setInputValue(value || '')
    }, [value])

    return <FlexRow>
        {!editing && <b>{children || inputValue}</b>}
        {editing && <AutocompleteSelect
            withArrow={false}
            mode={MODE_MINI}
            label={label}
            data={ids}
            value={inputValue}
            labels={labels}
            onSelect={setInputValue}
            onCancel={() => {
                setEditing(false)
            }}
        />
        }
        {!saving && <Button flat={true} round={true} className={classNames(smallIconButtonCss, css`margin-left: 10px;`)} onClick={e => {
            setEditing(!editing);
            if (editing) {
                setSaving(true)
                const promise = onSave(inputValue);
                if (!promise)
                    setSaving(false)
                else
                    promise.then(() => setSaving(false))
            }
        }}>
            {!editing && <MaterialIcon icon={'edit'}/>}
            {editing && <MaterialIcon icon={'done'}/>}
        </Button>}

        {saving && <SmallSpinner/>}
    </FlexRow>
}

const ListAlbums = ({artist}: { artist: ArtistDto }) => {
    const saveArtistName = async (value) => {
        await NetworkService.updateArtist({...artist, name: value}).then(ArtistsStore.set)
    }

    const [selected, setSelected] = useState([])
    const [merging, setMerging] = useState(false)

    const mergeAlbums = async () => {
        setMerging(true)
        const albums = [...selected]
        albums.sort(Comparators.of(id => getAlbumDuration(artist.albums.find(it => it.id === id)), Comparators.SORT_ASC, selected))
        const intoAlbumId = albums.pop()
        await NetworkService.mergeAlbums({artistId: artist.id, intoAlbumId, albums})
            .then(ArtistsStore.set)
            .catch(console.error)
        setMerging(false)
        setSelected([])
    }

    return <FlexColumn>
        {artist && <EditableTitle onSave={saveArtistName} value={artist?.name}/>}

        <span className={css`height: 25px;`}/>

        <Table<AlbumDto>
            sortBy={'name'}
            data={artist?.albums || []}
            onRowClick={(it, e) => {
                // @ts-ignore
                if (e.target?.closest('.Checkbox'))
                    return

                pushLocation('/edit/artists/' + artist.id + '/' + it.name + '/')
            }}
            rowClassName={css`
              &:hover {
                cursor: pointer;
              }
            `}
            columns={[
                {
                    field: 'coverPath',
                    sortable: false,
                    formatter: ((it, item) => <AlbumCover artistId={artist.id} album={item} forceShow={true}/>)
                },
                {
                    field: 'name',
                    header: 'Name',
                    sortable: true,
                },
                {
                    field: 'date',
                    header: 'Date',
                    sortable: true,
                },
                {
                    field: 'songs',
                    header: 'Tracks',
                    sortable: false,
                    formatter: ((it) => String(it.length))
                } as TableColumnTyped<Album, 'songs'>,
                {
                    field: 'songs',
                    header: 'Duration',
                    sortable: false,
                    formatter: ((it) => formatDuration(it.reduce((total, it) => {
                        total += it.duration;
                        return total
                    }, 0) || 0))
                } as TableColumnTyped<Album, 'songs'>,
                {
                    field: 'id',
                    header: '',
                    sortable: false,
                    formatter: ((id) => <Checkbox value={selected.includes(id)} onChange={e => {
                        if (e.target.checked) {
                            setSelected([...selected, id])
                        } else {
                            setSelected(selected.filter(it => it !== id))
                        }
                    }}/>)
                },
                {
                    field: 'id',
                    sortable: false,
                    formatter: ((id, item) => <Button round flat className={css`color: grey;`} onClick={e => {
                        DialogStore.show({
                            title: 'Are you sure to remove album?',
                            description: <>
                                <b>{item.name}</b>
                                <br/><br/>
                                This action cannot be undone!
                            </>,
                            buttons: <>
                                <Button className={'red'} onClick={() => {
                                    NetworkService.deleteAlbum({artistId: artist.id, albumId: item.id}).then(() => {
                                        NetworkService.getArtist({id: artist.id}).then(ArtistsStore.set)
                                        DialogStore.hide()
                                    })
                                }}>delete</Button>
                            </>,
                        })
                    }}>
                        <MaterialIcon icon={'delete'}/>
                    </Button>)
                }
            ]}/>

        <span className={css`height: 25px;`}/>

        <FlexRow className={css`justify-content: flex-end;`}>
            <Button className={css`margin-right: 20px;`} onClick={e => {
                const Form = ({}) => {
                    const [name, setName] = useState('')

                    const onCreateClick = e => {
                        NetworkService.createAlbum({artistId: artist.id, name}).then((artist) => {
                            ArtistsStore.set(artist)
                            DialogStore.hide()
                        })
                    }

                    return <>
                        <TextField value={name} onChange={e => setName(e.target.value)}/>
                        <FlexRow className={css`
                          justify-content: flex-end;
                          margin-top: 20px;
                        `}>
                            <Button onClick={onCreateClick}>Create</Button>
                        </FlexRow>
                    </>
                }

                DialogStore.show({
                    title: 'Create new album for artist ' + artist.name,
                    description: <Form/>,
                })
            }}>
                Create
            </Button>

            <Button disabled={merging || selected.length < 2} onClick={mergeAlbums}>
                {merging && <SmallSpinner/>}
                Merge
            </Button>
        </FlexRow>
    </FlexColumn>
}

const ListSongs = ({artist, album}: { artist: ArtistDto, album: AlbumDto }) => {
    const artistsStore = useStore(ArtistsStore.store)
    const saveAlbumName = async (value) => {
        const albums = [...artist.albums];
        const i = albums.findIndex(it => it.id === album.id);
        albums[i] = {...albums[i], name: value}
        await NetworkService.updateArtist({...artist, albums}).then(ArtistsStore.set)
        replaceLocation('/edit/artists/' + artist.id + '/' + encodeURIComponent(value))
    }

    const moveAlbum = async (toArtist) => {
        await NetworkService.moveAlbum({artistId: artist.id, albumId: album.id, toArtistId: toArtist})
            .then(() => {
                NetworkService.getArtists().then(ArtistsStore.setAll)
            })
            .catch(console.error)
    }

    const saveSongName = async (song: AlbumDtoSong, finishEditing: () => void) => {
        const proxy = createProxy(artist)
        const a = proxy.albums.find(it => it.id === album.id);
        const songIndex = a.songs.findIndex(it => it.id === song.id);
        a.songs[songIndex].title = song.title
        a.songs[songIndex].track = song.track

        let props = proxy.bake();
        // console.log(artist, props)
        await NetworkService.updateArtist(props).then(ArtistsStore.set)
        finishEditing()
    }

    return <>
        <FlexRow className={css`padding: 25px 50px;`}>
            <Dropzone multiple={false} accept={'image/*'} onDrop={files => {
                const file = files[0]
                if (!file)
                    return

                NetworkService.uploadCoverArt({file, artistId: artist.id, albumId: album.id})
                    .then(ArtistsStore.set)
            }}>
                <AlbumCover artistId={artist.id} album={album} forceShow={true}/>
            </Dropzone>

            <span className={css`width: 25px;`}/>

            <FlexColumn className={css`
              flex-basis: 1px;
              flex-grow: 1;
            `}>
                {album && artist && <EditableTitle onSave={saveAlbumName} value={album.name}/>}

                <span className={css`height: 5px;`}/>

                {artist && <EditableTitleSelect onSave={moveAlbum}
                                                label={"Select an Artist"}
                                                value={artist.id}
                                                ids={artistsStore.ids}
                                                labels={id => artistsStore.map[id]?.name}
                >
                    <span className={css`color: dimgray;
                      font-size: 14px;`}>by {artist.name}</span>
                </EditableTitleSelect>}

                <span className={css`height: 10px;`}/>

                <FlexRow>
                    tracks: {album?.songs?.length}
                    <span className={css`width: 25px;`}/>
                    duration: {formatDuration(getAlbumDuration(album))}
                </FlexRow>
            </FlexColumn>
        </FlexRow>
        <Table
            sortBy={'track'}
            data={album?.songs || []}
            onChange={saveSongName}
            columns={[
                {
                    field: 'track',
                    header: 'Track',
                    sortable: true,
                    editable: true,
                },
                {
                    field: 'title',
                    header: 'Title',
                    sortable: true,
                    editable: true,
                },
                {
                    field: 'duration',
                    header: 'Duration',
                    sortable: true,
                    formatter: formatDuration
                },
                {
                    field: 'id',
                    sortable: false,
                    formatter: ((id, item) => <Button round flat className={css`color: grey;`} onClick={e => {
                        DialogStore.show({
                            title: 'Are you sure to remove song?',
                            description: <>
                                <b>{item.title}</b>
                                <br/><br/>
                                This action cannot be undone!
                            </>,
                            buttons: <>
                                <Button className={'red'} onClick={() => {
                                    NetworkService.deleteSong({artistId: artist.id, albumId: album.id, songId: item.id}).then(() => {
                                        NetworkService.getArtist({id: artist.id}).then(ArtistsStore.set)
                                        DialogStore.hide()
                                    })
                                }}>delete</Button>
                            </>,
                        })
                    }}>
                        <MaterialIcon icon={'delete'}/>
                    </Button>)
                }
            ]}/></>
}
