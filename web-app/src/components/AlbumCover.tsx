import {useBlobUrl, useIsShownOnScreen, useWebCache} from "../utils/Hooks";
import React, {useEffect, useRef, useState} from "react";
import {css} from "goober";
import {classNames} from "react-ui-basics/Tools";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import NetworkService, {AlbumDto} from "../services/NetworkService";

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
};
const AlbumCover = ({artistId, album, className}: AlbumCoverProps) => {
    const src = album?.coverPath ? NetworkService.baseurl + '/artists/' + artistId + '/' + album.id + '/' + album.coverPath : null;
    const [loaded, setLoaded] = useState<boolean>()
    const ref = useRef<HTMLDivElement>();
    const isShownOrLoaded = useIsShownOnScreen(ref.current) || loaded
    const buffer = useWebCache(isShownOrLoaded && src ? src : null);
    const blobUrl = useBlobUrl(buffer)

    useEffect(() => {
        buffer && setLoaded(true)
    }, [buffer])

    return <div ref={ref} className={classNames('AlbumCover', AlbumCoverStyles, className)}>
        {blobUrl && <img src={blobUrl} alt={album?.name}/>}
        {!blobUrl && <MaterialIcon icon={'album'}/>}
    </div>
}

export default AlbumCover