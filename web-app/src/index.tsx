import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './components/App';
import {setup} from "goober";
// @ts-ignore
import {jsx} from 'react/jsx-runtime';

setup(jsx);

ReactDOM.render(<App/>, document.getElementById('root'));