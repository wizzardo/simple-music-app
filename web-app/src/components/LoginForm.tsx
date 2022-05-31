import React, {useState} from "react";
import TextField from "react-ui-basics/TextField";
import Button from "react-ui-basics/Button";
import {css} from "goober";
import {FlexColumn, FlexRow} from "./SharedComponents";
import NetworkService from "../services/NetworkService";
import * as AuthenticationStore from "../stores/AuthenticationStore";

const LoginForm = ({}) => {
    const [username, setUsername] = useState<string>('')
    const [password, setPassword] = useState<string>('')

    return <form className={css`
      margin-left: auto;
      margin-right: auto;
      width: 300px;
      margin-top: 5vh;
    `} onSubmit={async e => {
        e.preventDefault()
        try {
            const response = await NetworkService.login({username, password})
            AuthenticationStore.setTokenValidUntil(response.validUntil)
        } catch (e) {
            console.log('catch', e, e.status)
        }
    }}>
        <FlexRow>
            <img className={css`
              max-width: 300px;
            `} src={'/static/icon-512x512.png'}/>
        </FlexRow>

        <FlexColumn className={css`
          background: white;
          padding: 30px;
          border-radius: 4px;
          align-items: center;
        `}>
            <TextField label={'username'} name={'username'} onChange={e => setUsername(e.target.value)}/>
            <br/>
            <br/>
            <TextField label={'password'} name={'password'} type={'password'} onChange={e => setPassword(e.target.value)}/>
            <br/>
            <br/>
            <Button className={'blue'} type={'submit'}>
                Login
            </Button>
        </FlexColumn>
    </form>
}

export default LoginForm