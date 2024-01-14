import React, {useEffect, useState, createElement, FunctionComponent, ComponentClass} from 'react';

const Lazy = ({promise, ...other}: { promise: Promise<any> }) => {
    const [component, setComponent] = useState<{ c: FunctionComponent | ComponentClass }>()
    useEffect(() => {
        promise.then(({default: component}) => setComponent({c: component}));
    }, [])
    if (!component)
        return null;

    return createElement(component.c, other)
}

export default Lazy;