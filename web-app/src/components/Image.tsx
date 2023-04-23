import {useIsShownOnScreen, useWebCache} from "../utils/Hooks";
import {useEffect, useRef, useState} from "react";

const Image = ({src, alt, className}) => {
    const [blobUrl, setBlobUrl] = useState<string>()
    const ref = useRef<HTMLImageElement>();
    const isShownOrLoaded = useIsShownOnScreen(ref.current) || !!blobUrl
    const buffer = useWebCache(isShownOrLoaded ? src : null);

    useEffect(() => {
        if (!buffer)
            return;

        const blob = new Blob([buffer]);
        const blobUrl = URL.createObjectURL(blob);
        setBlobUrl(blobUrl);
        return () => URL.revokeObjectURL(blobUrl);
    }, [buffer])

    return <img ref={ref} src={(isShownOrLoaded && blobUrl) || ''} alt={alt} className={className}/>
}

export default Image