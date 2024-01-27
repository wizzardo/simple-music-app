import React, {useEffect} from 'react';
import {classNames} from "react-ui-basics/Tools";
import Route from "react-ui-basics/router/Route";
import LibraryEditor from "./lazy/LibraryEditor";
import Dialog from "./Dialog";
import Library from "./Library";
import Player from "./Player";
import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import * as PlayerStore from "../stores/PlayerStore";
import * as AuthenticationStore from "../stores/AuthenticationStore";
import {css} from "goober";
import NetworkService from "../services/NetworkService";
import CacheStats from "./CacheStats";
import Settings from "./Settings";
import MoreMenu from "./MoreMenu";
import DownloadQueue from "./DownloadQueue";
import Button from "react-ui-basics/Button";
import MaterialIcon from "react-ui-basics/MaterialIcon";
import {useImageBlobUrl, useWindowSize} from "../utils/Hooks";
import {useLocalCache} from "../services/LocalCacheService";
import LoginForm from "./LoginForm";

export default () => {
    const artistsStore = useStore(ArtistsStore.store)
    const queue = useStore(PlayerStore.store, ({queue}) => queue)
    const position = useStore(PlayerStore.store, ({position}) => position)
    const playing = useStore(PlayerStore.store, ({playing}) => playing)

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);

    const backgroundSrc = playing && album && `${NetworkService.baseurl}/artists/${artist.id}/${album.id}/${album.coverPath}`;
    const coverBackgroundUrl = useImageBlobUrl(backgroundSrc)
    console.log('coverBackground', album, coverBackgroundUrl, backgroundSrc)
    const coverBackground = coverBackgroundUrl && css`
      background-image: url('${coverBackgroundUrl}');
    `;

    const windowSize = useWindowSize();

    const localCacheDB = useLocalCache();
    useEffect(() => {
        localCacheDB && localCacheDB.deleteUnusedSongData()
    }, [localCacheDB])

    useEffect(() => {
        navigator.serviceWorker.onmessage = (event) => {
            console.log(event)
            const data = event.data;
            if (data.type === 'FETCH') {
                let url = new URL(data.url);
                if (url.pathname === '/artists') {
                    ArtistsStore.setAll(data.data)
                } else if (/\/artists\/([0-9]+)/.exec(url.pathname)) {
                    ArtistsStore.set(data.data)
                }
            }
        };
    }, [])

    const authenticationState = useStore(AuthenticationStore.store);
    if(authenticationState.loginRequired === null)
        return null

    if (authenticationState.loginRequired && authenticationState.tokenValidUntil < new Date().getTime())
        return <LoginForm/>

    return (
        <div className={classNames("App", css`
          background: white;
          background-size: cover;
          background-position-x: center;
          background-position-y: center;
          max-width: 900px;
          margin-left: auto;
          margin-right: auto;
        `, coverBackground)}>
            <div className={css`
              box-sizing: border-box;
              min-height: ${windowSize.height}px;

              background: rgba(255, 255, 255, 0.45);
              backdrop-filter: blur(40px);
              -webkit-backdrop-filter: blur(40px);
            `}>
                <Route path={'/*'}>
                    {window.location.pathname.length > 1 && <Button className={classNames('', css`
                      position: absolute;
                      top: 10px;
                      left: 10px;
                      z-index: 1;

                      .MaterialIcon {
                        font-size: 20px;
                        color: gray;
                      }
                    `)} flat round onClick={e => {
                        window.history.back()
                    }}>
                        <MaterialIcon icon={'chevron_left'}/>
                    </Button>}
                </Route>

                <MoreMenu className={css`
                  position: absolute;
                  z-index: 10;
                  right: 10px;
                  top: 10px;`}
                />

                <Route path={["/edit/artists/:artistId?/:album?", "/edit/albums"]}>
                    <LibraryEditor album={null} artistId={null}/>
                </Route>

                <Route path={"/cache"}>
                    <CacheStats/>
                </Route>

                <Route path={"/settings"}>
                    <Settings/>
                </Route>

                <Route path={["/:artistId(^[0-9]+$)?/:album?", "/", "/albums"]}>
                    <Library artistId={null} album={null}/>
                </Route>

                {queue.length > 0 && <Player/>}

                <Dialog/>
                <DownloadQueue/>
            </div>
        </div>
    );
}

