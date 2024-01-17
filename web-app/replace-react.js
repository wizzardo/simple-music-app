import fs from "fs-extra";

if (!fs.existsSync('buildts'))
    fs.mkdirSync('buildts');

const ls = (path, cb) => {
    fs.readdir(path, (err, files) => {
        if (err)
            return console.log('Unable to scan directory: ' + err);

        files.forEach(file => {
            let f = path + '/' + file;
            if (fs.lstatSync(f).isDirectory())
                ls(f, cb)
            else {
                cb(f)
            }
        });
    });
}

fs.rmSync('buildts/node_modules/prop-types', { recursive: true, force: true });

const removeSpacesInCSS = data => {
    const regexPattern = /\b[a-z]+(\("[a-z]+"\))? `/g;
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
    matches.reverse().forEach(i => {
        let end = data.indexOf('`', i)
        let css = data.substring(i, end)
        css = css.replaceAll(new RegExp(/\n\s+/g), '')
        css = css.replaceAll(new RegExp(/: /g), ':')
        css = css.replaceAll(new RegExp(/ \{/g), '{')
        data = data.substring(0, i) + css + data.substring(end)
        // console.log(data.substring(i, end))
    })
    return data;
};

ls('buildts', file => {
    if (!file.endsWith('.js'))
        return

    console.log(file)

    let data = ''
    data = fs.readFileSync(file, {encoding: 'utf8', flag: 'r'});

    data = data.replace(/from '(\.\.\/)*(node_modules\/)?react(-dom)?\/index.js'/, `from 'preact/compat'`)
    data = data.replace(/import '(\.\.\/)*(node_modules\/)?react(-dom)?\/index.js'/, `import 'preact/compat'`)
    data = data.replace('import react from \'../index.js\'', `import {default as react} from 'preact/compat'`)
    data = data.replace(/import reactDom from '(\.\.\/)*react-dom\/index.js'/, `import {default as reactDom} from 'preact/compat'`)
    data = data.replace(/import propTypes from '(\.\.\/)*prop-types\/index.js'/, `import propTypes from 'prop-types'`)
    data = data.replace(/import PropTypes from '(\.\.\/)*prop-types\/index.js'/, `import PropTypes from 'prop-types'`)
    data = data.replace(/from '(\.\.\/)*(node_modules\/)?react\/jsx-runtime.js'/, `from 'preact/jsx-runtime'`)

    data = removeSpacesInCSS(data);


    if (file === 'buildts/node_modules/react/cjs/react-jsx-runtime.development.js') {
        data = data.replace('import \'../index.js\'', `import 'preact/compat'`)
    }

    fs.writeFileSync(file, data, {encoding: "utf8",})
})
