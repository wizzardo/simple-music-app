import {useRef, useState} from "react";
import DropFileInput from 'react-ui-basics/DropFileInput'
import Button from 'react-ui-basics/Button'
import MaterialIcon from 'react-ui-basics/MaterialIcon'
import SpinningProgress from 'react-ui-basics/SpinningProgress'
import {classNames, debounce, orNoop} from "react-ui-basics/Tools";
import NetworkService from "../services/NetworkService";
import './UploadForm.scss'
import {css} from "goober";
import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import {FlexRow} from "./SharedComponents";

const FilesTableClass = css({
    width: '100%',
});

interface SongFile {
    artistId: number | null;
    albumId: string | null;
    cancel: () => void;
    progress: number;
    name: string;
    finished: boolean
    uploading: boolean
    error: boolean
    file: File
}

export const UploadForm = ({artistId, albumId}: { artistId?: number, albumId?: string }) => {
    const [files, setFiles] = useState<SongFile[]>([])
    const [uploading, setUploading] = useState(false)
    const artistsStore = useStore(ArtistsStore.store)

    let artist = artistId && artistsStore.map[artistId];
    let album = albumId && artist?.albums.find(it => it.id === albumId);

    const filesRef = useRef<SongFile[]>()
    filesRef.current = files

    const startUploadingNextFile = () => {
        const next = filesRef.current.find(it => !it.finished && !it.uploading);
        if (!next) {
            setUploading(false)
            return
        }

        const update = (newProps) => {
            setFiles(files => {
                const index = files.findIndex(it => it.file === next.file);
                if (index === -1)
                    return files;

                const nextFiles = files.slice();
                let nextState = {
                    ...nextFiles[index],
                    ...newProps
                };
                nextFiles[index] = nextState;
                console.log('set', nextState)
                return nextFiles
            })
        }

        update({uploading: true})
        NetworkService.upload({file: next.file, artistId: next.artistId, albumId: next.albumId}, {
            provideCancel: (cancel) => {
                update({cancel})
            },
            onProgress: debounce((e) => {
                    update({
                        progress: Math.floor(100 * e.loaded / e.total),
                        loaded: e.loaded,
                        total: e.total
                    })

                    // if (e.total - e.loaded === 0)
                    //     setTimeout(startUploadingNextFile, 100)
                }
                , 16)
        }).then(value => {
            update({
                progress: 100,
                finished: true
            })
            ArtistsStore.set(value)
            startUploadingNextFile()
        }).catch(reason => {
            console.log('upload failed for ', next)
            update({error: true, uploading: false, finished: false})
        })

    }

    return <div className={classNames('UploadForm', css`
      margin-top: 20px;
    `)}>
        <table className={FilesTableClass}>
            <tbody>
            {files.map(file => {
                let artist = artistsStore.map[file.artistId];
                let album = file.albumId && artist?.albums.find(it => it.id === file.albumId);
                return <tr className={'file'} key={file.name}>
                    <td>
                        <FlexRow>
                            {file.name}

                            {artist && <>
                                <MaterialIcon icon={'chevron_right'}/> <b>{artist.name}</b>
                            </>}
                            {album && <>
                                <MaterialIcon icon={'chevron_right'}/> <b>{album.name}</b>
                            </>}
                        </FlexRow>
                    </td>
                    <td>
                        {file.uploading && !file.finished && file.progress !== 100 && 'uploading'}
                        {file.uploading && !file.finished && file.progress === 100 && 'processing'}
                        {file.uploading && file.finished && 'ok'}
                        {file.error && 'error'}
                    </td>
                    <td>
                        {file.uploading && !file.finished && file.progress !== 100 && `${file.progress}%`}
                        {file.uploading && !file.finished && file.progress === 100 && <SpinningProgress/>}
                    </td>
                    <td></td>
                    <td>
                        <Button round={true} flat={true} raised={false} disabled={file.finished} onClick={e => {
                            const index = files.findIndex(it => it.file === file.file);
                            if (index === -1)
                                return;

                            orNoop(files[index].cancel)()
                            setFiles(files.filter((it, i) => i !== index))
                            startUploadingNextFile()
                        }}>
                            <MaterialIcon icon={'close'}/>
                        </Button>
                    </td>
                </tr>
            })}
            </tbody>
        </table>

        {!uploading && <DropFileInput
            label={<FlexRow>
                Select files to upload

                {artist && <>
                    <MaterialIcon icon={'chevron_right'}/> <b>{artist.name}</b>
                </>}
                {album && <>
                    <MaterialIcon icon={'chevron_right'}/> <b>{album.name}</b>
                </>}
            </FlexRow>}
            multiple={true}
            accept={'audio/*'}
            onDrop={newFiles => {
                setFiles([...files, ...newFiles.map(file => ({
                    file,
                    artistId,
                    albumId,
                    name: file.name,
                    size: file.size,
                    total: file.size,
                    progress: 0,
                    loaded: 0,
                    cancel: null,
                    finished: false,
                    uploading: false,
                    error: false,
                }))]);
            }}/>}

        <br/>
        <Button className={'blue'} disabled={!files.length || uploading} onClick={e => {
            setUploading(true);
            startUploadingNextFile()
        }}>upload</Button>

    </div>
}