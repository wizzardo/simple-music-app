import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './components/App';
import {setup} from "goober";
// @ts-ignore
import {jsx} from 'react/jsx-runtime';
import BuildInfo from "./BuildInfo";

setup(jsx);
window['buildInfo'] = BuildInfo;

ReactDOM.render(<App/>, document.getElementById('root'));

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js');
}