import {css, styled} from "goober";

export const FlexRow = styled('div')`
  display: flex;
  flex-flow: row nowrap;
  align-items: center;
`
export const FlexColumn = styled('div')`
  display: flex;
  flex-flow: column nowrap;
  justify-content: center;
`
export const smallIconButtonCss = css`
  color: darkgray;
  min-width: 24px !important;
  min-height: 24px;

  .material-icons {
    font-size: 16px;
  }
`;