import {defineConfig} from 'vite'
import MagicString from 'magic-string';

const removeSpacesInCSS = data => {
    // const regexPattern = /\b[a-z]+(\("[a-z]+"\))? `/g;
    const regexPattern = /\b(css|styled)(\("[a-z]+"\))? *`/g;
    let regex = new RegExp(regexPattern);
    let lastIndex = -1;
    let match
    let matches = []
    while ((match = regex.exec(data)) !== null) {
        let i = match.index;
        i = data.indexOf('`', i) + 1
        matches.push(i)

        lastIndex = i;
    }
    const magicString = new MagicString(data);
    const srcLength = data.length;
    matches.reverse().forEach(i => {
        let end = data.indexOf('`', i)
        let css = data.substring(i, end)
        css = css.replaceAll(new RegExp(/\n\s*/g), '')
        css = css.replaceAll(new RegExp(/: /g), ':')
        css = css.replaceAll(new RegExp(/ \{/g), '{')
        data = data.substring(0, i) + css + data.substring(end)
        // console.log(data.substring(i, end))
    })

    magicString.overwrite(0, srcLength, data);
    // console.log(data)
    return {
        code: magicString.toString(),
        map: magicString.generateMap({hires: true}),
    };
};

const removeSpacesInCSSPlugin = () => ({
    name: 'transform-plugin',
    transform(code, id) {
        if (id.endsWith('.ts') || id.endsWith('.tsx')) {
            return removeSpacesInCSS(code);
        }
        return null;
    },
})


export default defineConfig({
    root: "src",
    build: {
        outDir: '../build',
        emptyOutDir: true,
        sourcemap: true,
    },
    esbuild: {
        jsxFactory: 'h',
        jsxFragment: 'Fragment',
        jsxInject: `import { h, Fragment } from 'preact'`,
    },
    plugins: [removeSpacesInCSSPlugin()],
    resolve: {
        alias: {
            "react": 'preact/compat',
            "react-dom": 'preact/compat',
        },
    },
})