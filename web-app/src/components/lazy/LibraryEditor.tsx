import Lazy from "../Lazy";
import {LibraryEditorProps} from "../LibraryEditor.tsx";

export default (props: LibraryEditorProps) => {
    return (<Lazy {...props} promise={import("../LibraryEditor.tsx")}/>);
};
