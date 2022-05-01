import resolve from 'rollup-plugin-node-resolve';
import commonjs from 'rollup-plugin-commonjs';
import json from 'rollup-plugin-json';
import replace from 'rollup-plugin-replace';
import styles from "rollup-plugin-styles";
import typescript from '@rollup/plugin-typescript';

const env = process.env.NODE_ENV || 'production';
// const env = 'development';
const isProd = env === 'production';
const isDev = !isProd;


export default {
    input: 'src/index.tsx',
    output: [
        {
            dir: `buildts`,
            format: 'es',
            assetFileNames: "[name][extname]",
        },
    ],
    preserveModules: true,
    treeshake: true,
    inlineDynamicImports: isDev || false, // true = disabling code splitting to chunks
    perf: false,
    plugins: [
        styles({
            mode: ["extract", "bundle.css"],
        }),
        resolve({
            browser: true,
        }),
        json(),
        commonjs({
            include: [
                'node_modules/**',
            ],
            exclude: [
                'node_modules/process-es6/**',
            ],
            namedExports: {
                'node_modules/react/index.js': ['Children', 'Component', 'PropTypes', 'createElement', 'PureComponent',
                    'useLayoutEffect', 'useEffect', 'useState', 'useMemo', 'useContext', 'useReducer', 'useRef'],
                'node_modules/react-is/index.js': ['isValidElementType', 'isContextConsumer'],
                'node_modules/react-redux/node_modules/react-is/index.js': ['isValidElementType', 'isContextConsumer'],
                'node_modules/react-dom/index.js': ['render', 'unstable_batchedUpdates'],
                'node_modules/react/jsx-runtime.js': ['jsx', 'jsxs', 'Fragment'],
            },
        }),
        typescript(),
        replace({
            'process.env.NODE_ENV': JSON.stringify(env),
        }),
    ],
};
