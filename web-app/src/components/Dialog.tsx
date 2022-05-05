import React, {useEffect, useState} from 'react';
import DialogStore from "../stores/DialogStore";
import Modal from 'react-ui-basics/Modal'
import 'react-ui-basics/Dialog.css'
import {classNames} from "react-ui-basics/Tools";
import Button from "react-ui-basics/Button";
import {useStore} from "react-ui-basics/store/Store";
import MaterialIcon from "react-ui-basics/MaterialIcon";

export default () => {
    const {show, accept, cancel, onAccept, onCancel, title, description, buttons} = useStore(DialogStore)

    const [open, setOpen] = useState();
    const [close, setClose] = useState();

    useEffect(() => {
        if (show) {
            // @ts-ignore
            open && open()
        } else {
            // @ts-ignore
            close && close()
        }
    }, [show])

    // console.log('Dialog.render', show)

    return <Modal
        className={classNames('DialogModal')}
        open={open => setOpen(() => open)}
        close={close => setClose(() => close)}
        onClose={onCancel}
        closeIcon={<MaterialIcon icon={'close'}/>}
    >
        <div className="Dialog">
            {title && <div className="title">{title}</div>}
            {description && <div className="description">{description}</div>}
            <div className="row end">
                {cancel && <Button flat={true} onClick={onCancel}>{cancel}</Button>}
                {accept && <Button flat={true} onClick={onAccept}>{accept}</Button>}
                {buttons}
            </div>
        </div>
    </Modal>;
}