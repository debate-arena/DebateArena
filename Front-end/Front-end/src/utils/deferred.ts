export class Deferred<T = void> {
    promise: Promise<T>
    resolve!: (val: T) => void
    reject!: (err: any) => void
    constructor() {
      this.promise = new Promise<T>((res, rej) => {
        this.resolve = res
        this.reject  = rej
      })
    }
  }
  