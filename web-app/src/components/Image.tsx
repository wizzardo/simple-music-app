import {useBlobUrl, useIsShownOnScreen, useWebCache} from "../utils/Hooks";
import {useEffect, useRef, useState} from "react";

const Image = ({src, alt, className}) => {
    const [loaded, setLoaded] = useState<boolean>()
    const ref = useRef<HTMLImageElement>();
    const isShownOrLoaded = useIsShownOnScreen(ref.current) || loaded
    const buffer = useWebCache(isShownOrLoaded ? src : null);
    const blobUrl = useBlobUrl(buffer)

    useEffect(() => {
        buffer && setLoaded(true)
    }, [buffer])

    return <img ref={ref} src={blobUrl} alt={alt} className={className}/>
}

export default Image