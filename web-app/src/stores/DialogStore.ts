import {ElementType} from 'react'
import {Store} from 'react-ui-basics/store/Store'

export interface DialogState {
    show: boolean,
    accept: string | ElementType,
    cancel: string | ElementType,
    title: string | ElementType,
    description: string | ElementType,
    buttons: ElementType,
    onAccept: () => void,
    onCancel: () => void,
}

export const store = new Store<DialogState>({
    show: false,
    accept: "",
    cancel: "",
    title: "",
    description: "",
    buttons: null,
    onAccept: () => {
    },
    onCancel: () => {
    },
})

export default store;

export const show = ({title, description, accept, onAccept, cancel, onCancel, buttons}: { title?, description?, accept?, onAccept?, cancel?, onCancel?, buttons? }) => {
    store.set(state => Object.assign(state, {
        show: true,
        accept,
        cancel,
        onAccept,
        title,
        description,
        buttons,
        onCancel: onCancel && (() => {
            onCancel && onCancel()
            hide()
        })
    }));
}
export const hide = () => {
    store.set(state => Object.assign(state, {
        show: false,
        accept: null,
        cancel: null,
        onAccept: null,
        title: null,
        description: null,
        buttons: null,
        onCancel: null
    }));
}
