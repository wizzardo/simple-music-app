import {useBlobUrl, useIsShownOnScreen, useWebCache} from "../utils/Hooks";
import React, {useEffect, useRef, useState} from "react";
import {css} from "goober";
import {classNames} from "react-ui-basics/Tools";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import NetworkService, {AlbumDto} from "../services/NetworkService";
import * as BlobStore from "../stores/BlobStore";
import {useStore} from "react-ui-basics/store/Store";

const AlbumCoverStyles = css`
  &.AlbumCover {
    height: 150px;
    display: flex;
    justify-content: center;
    align-items: center;

    img {
      max-width: 100%;
      max-height: 150px;
      border-radius: 4px;
    }

    .MaterialIcon {
      font-size: 80px;
    }

    &.mobile {
      height: 300px;

      img {
        width: 100%;
        max-height: 300px;
        max-width: 300px;
        margin-left: auto;
        margin-right: auto;
        margin-bottom: 20px;
      }
    }

    &.small {
      height: 75px;

      img {
        border-radius: 0;
        max-width: 75px;
        max-height: 75px;
        min-width: 75px;
        min-height: 75px;
      }
    }
  }
`

type AlbumCoverProps = {
    artistId: number,
    album?: AlbumDto,
    className?: 'small' | 'mobile' | string
    forceShow?: boolean
};
const AlbumCover = ({artistId, album, className, forceShow}: AlbumCoverProps) => {
    const src = album?.coverPath ? NetworkService.baseurl + '/artists/' + artistId + '/' + album.id + '/' + album.coverPath : null;
    const ref = useRef<HTMLDivElement>();
    const blobUrl = useStore(BlobStore.store, state => state[src])
    const isShown = useIsShownOnScreen(ref.current) || !!forceShow
    const buffer = useWebCache(isShown && !blobUrl && src ? src : null);

    useEffect(() => {
        if (!buffer || blobUrl) return

        const blob = new Blob([buffer]);
        BlobStore.add(src, URL.createObjectURL(blob))
    }, [buffer, blobUrl])

    return <div ref={ref} className={classNames('AlbumCover', AlbumCoverStyles, className)}>
        {blobUrl && <img src={blobUrl} alt={album?.name}/>}
        {!src && <MaterialIcon icon={'album'}/>}
    </div>
}

export default AlbumCover