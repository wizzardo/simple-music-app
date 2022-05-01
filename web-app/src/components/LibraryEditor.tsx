import {styled} from "goober";
import {css} from "goober";
import Route from "react-ui-basics/router/Route";
import Button from "react-ui-basics/Button";
import TextField from "react-ui-basics/TextField";
import Link from "react-ui-basics/router/Link";
import {pushLocation} from "react-ui-basics/router/HistoryTools";
import NetworkService, {ArtistDto} from "../services/NetworkService";
import {useEffect, useState} from "react";
import Table from "react-ui-basics/Table";
import ScrollableTable from "react-ui-basics/ScrollableTable";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import dayjs from "dayjs";

export const DateFormatter = (date, item, format = 'YY-MM-DD hh:mm:ss') => date && dayjs(date).format(format);

const LibraryEditor = ({artistId, album}) => {
    const [artists, setArtists] = useState<ArtistDto[]>([])
    useEffect(() => {
        NetworkService.getArtists().then(value => setArtists(value))
    }, [])

    const [artist, setArtist] = useState<ArtistDto>();
    useEffect( () => {
        if (artistId) {
            NetworkService.getArtist({id: artistId}).then(value => setArtist(value))
        }
    }, [artistId])

    let albumName = album && decodeURI(album);
    return <div>
        <Link href={'/edit/'}>Artists:</Link>
        &nbsp;&nbsp;
        {artistId && artist && <Link href={'/edit/' + artistId + '/'}>{artist.name}:</Link>}
        &nbsp;&nbsp;
        {albumName && <span>{albumName}:</span>}
        <br/>
        <br/>

        <Route path={"/edit"}>
            <ListArtists artists={artists}/>
        </Route>
        <Route path={"/edit/:artistId"}>
            <ListAlbums albums={artists.find(it => it.id == artistId)?.albums} artist={artists.find(it => it.id == artistId)}/>
        </Route>
        <Route path={"/edit/:artistId/:album"}>
            <ListSongs songs={artists.find(it => it.id == artistId)?.albums?.find(it => it.name === albumName)?.songs || []}/>
        </Route>
    </div>
}

export default LibraryEditor

const ListArtists = ({artists}) => {
    return <Table sortBy={'name'}
                  data={artists}
                  onRowClick={it => {
                      pushLocation(it['id'] + '/')
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
                      }
                  ]}/>
}


const Cover = styled("img")`
  border-radius: 4px;
  max-width: 150px;
  max-height: 150px;
`;

const FlexRow = styled('div')`
  display: flex;
  flex-flow: row nowrap;
  align-items: center;
`
const smallIconButtonCss = css`
  color: darkgray;
  min-width: 24px !important;
  min-height: 24px;

  .material-icons {
    font-size: 16px;
  }
`;

const EditableTitle = ({value, onSave}) => {
    const [editing, setEditing] = useState(false)
    const [inputValue, setInputValue] = useState()
    const [shouldFocus, setShouldFocus] = useState(true)


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
                                       onSave(inputValue)
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
        <Button flat={true} round={true} className={smallIconButtonCss} onClick={e => {
            setEditing(!editing);
            if (editing) {
                setShouldFocus(true)
                onSave(inputValue)
            }
        }}>
            {!editing && <MaterialIcon icon={'edit'}/>}
            {editing && <MaterialIcon icon={'done'}/>}
        </Button>
    </FlexRow>
}

const ListAlbums = ({albums, artist}) => {

    const saveArtistName = (value) => {
        NetworkService.updateArtist({...artist, name: value})
    }


    return <>
        {artist && <EditableTitle onSave={saveArtistName} value={artist?.name}/>}
        <Table sortBy={'name'}
               data={albums}
               onRowClick={it => {
                   pushLocation(it['name'] + '/')
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
                       formatter: (it => {
                           if (it)
                               return <Cover src={NetworkService.baseurl + '/artists/' + it} alt={'cover'}/>;
                           else
                               return <MaterialIcon className={css`
                                 font-size: 50px;
                               `} icon={'album'}/>;
                       })
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
                   }
               ]}/></>
}

const ListSongs = ({songs}) => {
    return <Table sortBy={'track'} data={songs} columns={[
        {
            field: 'track',
            header: 'Track',
            sortable: true,
        },
        {
            field: 'title',
            header: 'Title',
            sortable: true,
        },
        {
            field: 'duration',
            header: 'Duration',
            sortable: true,
            formatter: (it => {
                let minutes = Number(it / 1000 / 60).toFixed(0);
                let seconds = Number((it / 1000) % 60).toFixed(0);
                return <>{minutes + ':' + (seconds.length < 2 ? '0' + seconds : seconds)}</>;
            })
        }
    ]}/>
}
