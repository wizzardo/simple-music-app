import React from 'react';
import './App.css'
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

export default () => {
    const artistsStore = useStore(ArtistsStore.store)
    const {playing, position, queue} = useStore(PlayerStore.store)

    const queuedSong = queue[position]
    const artist = artistsStore.map[queuedSong?.artistId];
    const album = artist?.albums?.find(it => it.id === queuedSong?.albumId);
    const coverBackground = playing && album && css`
      background: url('${NetworkService.baseurl}/artists/${artist.path}/${album.path}/${album.coverPath}');
      background-size: cover;
      background-position-x: center;
      background-position-y: center;
    `;

    return (
        <div className={classNames("App", coverBackground)}>
            <div className="content">
                <Route path={"/edit/:artistId?/:album?"}>
                    <LibraryEditor album={null} artistId={null}/>
                </Route>

                <Route path={"/!edit/*"}>
                    <Route path={"/:artistId?/:album?"}>
                        <Library artistId={null} album={null}/>
                    </Route>
                    <Player/>
                </Route>

                <Dialog/>
            </div>
        </div>
    );
}

