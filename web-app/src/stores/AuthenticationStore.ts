import Store from "react-ui-basics/store/Store";
import NetworkService from "../services/NetworkService";

export type AuthenticationState = {
    tokenValidUntil: number,
    loginRequired: boolean | null
}

export const store = new Store({
    tokenValidUntil: 0,
    loginRequired: null
} as AuthenticationState)

export default store;

const saveSettings = () => {
    localStorage.setItem('login', JSON.stringify(store.get()));
};

export const setTokenValidUntil = (tokenValidUntil: number) => {
    store.set(state => {
        state.tokenValidUntil = tokenValidUntil
    });
    saveSettings()
}

(async () => {
    let savedState = localStorage.getItem('login');
    if (savedState) {
        store.set(() => JSON.parse(savedState))
    }

    if (!savedState || !JSON.parse(savedState).loginRequired) {
        const isLoginRequired = await NetworkService.isLoginRequired()
        store.set((state) => {
            state.loginRequired = isLoginRequired.required
        })
    }
})()
