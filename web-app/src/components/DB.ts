class DB {

    db: IDBDatabase;

    constructor() {
        let openDBRequest = window.indexedDB.open("MyTestDatabase", 2);
        openDBRequest.onerror = event => {
            console.error(event)
        };
        openDBRequest.onsuccess = event => {
            console.log(event)
            this.db = event.target['result'] as IDBDatabase;
        };
        openDBRequest.onupgradeneeded = event => {
            // Save the IDBDatabase interface
            this.db = event.target['result'];
            console.log('onupgradeneeded')

            // Create an objectStore for this database
            var objectStore = this.db.createObjectStore("name", {keyPath: "myKey"});
        };
    }

    get(path:string){
        let store = this.db.createObjectStore("name", {keyPath: path});
    }
}

export default DB;