import {useWebCache} from "../utils/Hooks";
import {useEffect, useState} from "react";


const Image = ({src, alt, className}) => {
    const buffer = useWebCache(src);

    const [blobUrl, setBlobUrl] = useState<string>()
    useEffect(() => {
        if (!buffer)
            return;

        const blob = new Blob([buffer]);
        const blobUrl = URL.createObjectURL(blob);
        setBlobUrl(blobUrl);
        return () => URL.revokeObjectURL(blobUrl);
    }, [buffer])

    if (!blobUrl)
        return null

    return <img src={blobUrl} alt={alt} className={className}/>
}

export default Image