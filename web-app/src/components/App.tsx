import React from 'react';
import {classNames} from "react-ui-basics/Tools";
import Route from "react-ui-basics/router/Route";
import LibraryEditor from "./LibraryEditor";
import Dialog from "./Dialog";
import Library from "./Library";
import Player from "./Player";
import {useStore} from "react-ui-basics/store/Store";
import * as ArtistsStore from "../stores/ArtistsStore";
import * as PlayerStore from "../stores/PlayerStore";
import {css} from "goober";
import NetworkService from "../services/NetworkService";
import CacheStats from "./CacheStats";
import Settings from "./Settings";
import MoreMenu from "./MoreMenu";
import DownloadQueue from "./DownloadQueue";

export default () => {
    const artistsStore = useStore(ArtistsStore.store)
    const {playing, position, queue} = useStore(PlayerStore.store)

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
    const coverBackground = playing && album && css`
      background-image: url('${NetworkService.baseurl}/artists/${artist.path}/${album.path}/${album.coverPath}');
    `;

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
              padding: 20px;
              padding-top: 40px;
              box-sizing: border-box;
              min-height: 100vh;
              padding-bottom: 125px;

              background: rgba(255, 255, 255, 0.45);
              backdrop-filter: blur(40px);
            `}>
                <MoreMenu className={css`
                  position: absolute;
                  right: 10px;
                  top: 10px;`}
                />

                <Route path={"/edit/:artistId?/:album?"}>
                    <LibraryEditor album={null} artistId={null}/>
                </Route>

                <Route path={"/cacheStats"}>
                    <CacheStats/>
                </Route>

                <Route path={"/settings"}>
                    <Settings/>
                </Route>

                <Route path={"/:artistId(^[0-9]+$)?/:album?"}>
                    <Library artistId={null} album={null}/>
                </Route>

                {queue.length > 0 && <Player/>}

                <Dialog/>
                <DownloadQueue/>
            </div>
        </div>
    );
}

