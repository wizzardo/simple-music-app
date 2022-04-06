import React from "react";
import './ProgressBar.css'
import {createRef} from "react-ui-basics/Tools";

type OnProgressClick = (progress: number) => void

const ProgressBar = ({progress = 0, onClick}: { progress: number, onClick?: OnProgressClick }) => {
    const el = createRef()
    return <div ref={el} className="ProgressBar" onClick={e => {
        console.log(e)

        var rect = el().getBoundingClientRect();
        let progress = (e.pageX - rect.x) * 100 / rect.width;
        console.log(e.pageX, rect.x, progress)
        onClick(progress)
    }}>
        <div className="progress bar bar1" style={{width: progress + '%'}}/>
        <div className="buffer bar bar2"/>
    </div>
}

export default ProgressBar;