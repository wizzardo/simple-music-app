import {fetch} from "./HttpClient";

const baseurl = 'http://192.168.0.147:8080'

type Params = { [id: string]: any };
type UrlMaker = (params: Params) => string;

const variablePatter = /\{(\w+)\}/g;
const createUrlMaker = (template: string, deleteVars: boolean = false): UrlMaker => {
    let parts = [];
    let variables = [];

    let find;
    let prevIndex = 0;
    while ((find = variablePatter.exec(template)) !== null) {
        variables.push(find[1]);
        parts.push(template.substring(prevIndex, find.index));
        prevIndex = find.index + find[0].length;
    }
    if (prevIndex === 0)
        return () => template;

    parts.push(template.substring(prevIndex, template.length));

    const m = parts;
    const v = variables;
    return function (params: any) {
        const length = Math.max(m.length, v.length);
        let s = '';
        for (let i = 0; i < length; i++) {
            if (m[i] !== null)
                s += m[i];
            let param = params[v[i]];
            if (params && v[i] !== null && param != null) {
                s += encodeURIComponent(param);
                if (deleteVars)
                    delete params[v[i]]
            }
        }
        return s;
    };
}

const lazy = <T extends Function>(f: ((...any) => T), ...args): any => {
    let result: T;
    return function (): any {
        if (!result)
            result = f.apply(null, args) as T

        return result.apply(null, arguments)
    };
}

const createGET = <R>(template: string) => {
    let urlMaker: (UrlMaker) = lazy(createUrlMaker, template, true);
    return async (params?: Params,) => fetch<R>(`${baseurl}${urlMaker(params)}`, {params});
};


const createPOST = <R, P extends Params>(template: string) => {
    let urlMaker: (UrlMaker) = lazy(createUrlMaker, template);
    return async (params?: P,) => fetch(`${baseurl}${urlMaker(params)}`, {params, method: "POST"});
};

const createDelete = <R>(template: string) => {
    let urlMaker: (UrlMaker) = lazy(createUrlMaker, template, true);
    return async (params?: Params,) => fetch(`${baseurl}${urlMaker(params)}`, {params, method: "DELETE"});
};

export interface MultipartOptions {
    onProgress?: ((ev: ProgressEvent) => any)
    provideCancel?: ((cancelFunction: () => void) => void)
}

const createMultipart = <R>(template: string) => {
    let urlMaker: (UrlMaker) = lazy(createUrlMaker, template);
    return async (params: any, options?: MultipartOptions) => {
        let url = `${baseurl}${urlMaker(params)}`;
        return fetch(url, {params, method: "POST", multipart: true, ...options});
    };
};

export default {
    baseurl,
    //generated endpoints start
    getAlbumCover: createGET<number[]>('/artists/{artistName}/{albumName}/cover.jpg'),
    getArtist: createGET<ArtistDto>('/artists/{id}'),
    getArtists: createGET<Array<ArtistDto>>('/artists'),
    getSong: createGET<number[]>('/artists/{artistId}/{albumName}/{trackNumber}'),
    getSongConverted: createGET<number[]>('/artists/{artistId}/{albumName}/{trackNumber}/{format}/{bitrate}'),
    updateArtist: createPOST<ArtistDto, ArtistDto>('/artists/{id}'),


    upload: createMultipart<Object>('/upload'),
//generated endpoints end
}

//generated types start
export interface ArtistDto {
    id: number,
    created: string,
    updated: string,
    name: string,
    albums: Array<AlbumDto>,
}

export interface AlbumDto {
    id: string,
    path: string,
    date: string,
    name: string,
    songs: Array<AlbumDtoSong>,
    coverPath: string | null,
    coverHash: string | null,
}

export interface AlbumDtoSong {
    id: string,
    track: number,
    title: string,
    comment: string,
    duration: number,
    streams: Array<string>,
    path: string,
}

//generated types end