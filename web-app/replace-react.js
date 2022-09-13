const fs = require("fs-extra")

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

ls('buildts', file => {
    if (!file.endsWith('.js'))
        return

    console.log(file)

    let data = fs.readFileSync(file, {encoding: 'utf8', flag: 'r'});

    data = data.replace(/from '(\.\.\/)*(node_modules\/)?react(-dom)?\/index.js'/, `from 'preact/compat'`)
    data = data.replace(/import '(\.\.\/)*(node_modules\/)?react(-dom)?\/index.js'/, `import 'preact/compat'`)
    data = data.replace('import react from \'../index.js\'', `import {default as react} from 'preact/compat'`)
    data = data.replace(/import reactDom from '(\.\.\/)*react-dom\/index.js'/, `import {default as reactDom} from 'preact/compat'`)
    data = data.replace(/import propTypes from '(\.\.\/)*prop-types\/index.js'/, `import propTypes from 'prop-types'`)
    data = data.replace(/import PropTypes from '(\.\.\/)*prop-types\/index.js'/, `import PropTypes from 'prop-types'`)
    data = data.replace(/from '(\.\.\/)*(node_modules\/)?react\/jsx-runtime.js'/, `from 'preact/jsx-runtime'`)


    if (file === 'buildts/node_modules/react/cjs/react-jsx-runtime.development.js') {
        data = data.replace('import \'../index.js\'', `import 'preact/compat'`)
    }

    fs.writeFileSync(file, data, {encoding: "utf8",})
})
