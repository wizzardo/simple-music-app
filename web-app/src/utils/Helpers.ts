import {AlbumDto} from "../services/NetworkService";


export const formatDuration = it => {
    let minutes = Math.floor(Number(it / 1000 / 60));
    let seconds = Math.floor((it / 1000) % 60);
    if (minutes < 60)
        return minutes + ':' + (seconds < 10 ? '0' + seconds : seconds);

    let hours = Math.floor(Number(minutes / 60))
    minutes = minutes % 60
    return hours + ':' + (minutes < 10 ? '0' + minutes : minutes) + ':' + (seconds < 10 ? '0' + seconds : seconds);
};

export const getAlbumDuration = (album: AlbumDto) => {
    return album?.songs?.reduce((total, it) => {
        total += it.duration;
        return total
    }, 0) || 0;
}
