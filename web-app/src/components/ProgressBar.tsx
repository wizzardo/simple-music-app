import React, {useEffect, useState} from "react";
import {classNames, createRef} from "react-ui-basics/Tools";
import {css} from "goober";

type OnProgressClick = (progress: number) => void

const ProgressBar = ({progress = 0, onClick, draggable}: { draggable: boolean, progress: number, onClick?: OnProgressClick }) => {
    const el = createRef<HTMLDivElement>()

    const [isDragging, setIsDragging] = useState(false)

    useEffect(() => {
        if (isDragging) {
            let removeListeners: () => void;
            let moveListener = e => {
                var rect = el().getBoundingClientRect();
                let progress = Math.min(Math.max((e.pageX - rect.x), 0) * 100 / rect.width, 100);
                onClick(progress)
            };
            let upListener = ev => {
                setIsDragging(false)
                removeListeners()
            };
            removeListeners = () => {
                document.removeEventListener('mouseup', upListener)
                document.removeEventListener('mousemove', moveListener)
            };
            document.addEventListener('mouseup', upListener)
            document.addEventListener('mousemove', moveListener)
            return removeListeners
        }
    }, [isDragging])

    return <div ref={el}
                className={classNames("ProgressBar", css`
                  padding-top: 5px;
                  padding-bottom: 5px;
                  width: 100%;
                  display: block;
                  position: relative;
                  height: 4px;
                  cursor: pointer;

                  > .progress {
                  position: relative;
                    background-color: #3f51b5;
                    z-index: 1;
                    left: 0;

                    &:after {
                      background-color: #3f51b5;
                      content: '';
                      position: absolute;
                      right: -5px;
                      top: -3px;
                      width: 10px;
                      height: 10px;
                      border-radius: 50%;
                    }
                  }

                  > .bar {
                    max-width: 100%;
                    display: block;
                    position: absolute;
                    height: 4px;
                    width: 0%;

                    &.bg {
                      width: 100%;
                      background: gray;
                      z-index: 0;
                      left: 0;
                    }
                  }
                `)}
                onClick={e => {
                    var rect = el().getBoundingClientRect();
                    let progress = (e.pageX - rect.x) * 100 / rect.width;
                    // console.log(e.pageX, rect.x, progress)
                    onClick(progress)
                }}
                onMouseDown={e => setIsDragging(!!draggable)}
    >
        <div className="progress bar" style={{width: progress + '%'}}/>
        <div className="bar bg"/>
    </div>
}

export default ProgressBar;