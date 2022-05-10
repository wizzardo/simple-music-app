import {useEffect, useState} from "react";
import {WINDOW} from "react-ui-basics/Tools";

export const useWindowSize = () => {
    const [windowsSize, setWindowSize] = useState({width: WINDOW.innerWidth, height: WINDOW.innerHeight})

    useEffect(() => {
        const listener = () => {
            setWindowSize({width: WINDOW.innerWidth, height: WINDOW.innerHeight})
        }
        WINDOW.addEventListener('resize', listener)
        return () => WINDOW.removeEventListener('resize', listener)
    }, [])
    return windowsSize
}