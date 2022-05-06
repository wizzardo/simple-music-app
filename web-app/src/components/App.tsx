import React from 'react';
import './App.css'
import {classNames} from "react-ui-basics/Tools";
import Route from "react-ui-basics/router/Route";
import LibraryEditor from "./LibraryEditor";
import Dialog from "./Dialog";
import Library from "./Library";
import Player from "./Player";

export default () => {

    return (
        <div className={classNames("App")}>
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

