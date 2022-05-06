import Store from "react-ui-basics/store/Store";

const document = window.document;
let hidden, visibilityChange;
// @ts-ignore
if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support
    hidden = "hidden";
    visibilityChange = "visibilitychange";
} else { // @ts-ignore
    if (typeof document.msHidden !== "undefined") {
        hidden = "msHidden";
        visibilityChange = "msvisibilitychange";
    } else { // @ts-ignore
        if (typeof document.webkitHidden !== "undefined") {
            hidden = "webkitHidden";
            visibilityChange = "webkitvisibilitychange";
        }
    }
}

const isHidden = hidden ? (() => document[hidden]) : (() => false);
const isActive = () => !isHidden();

if (hidden && visibilityChange)
    document.addEventListener(visibilityChange, () => {
        store.set(() => ({hidden: isHidden()}));
    }, false);

export type WindowActiveState = {
    hidden: boolean
}

export const store = new Store({
    hidden: isHidden(),
} as WindowActiveState)

export default store;