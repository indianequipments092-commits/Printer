export interface IppTransport {
  sendPrintJob(uri: string, document: Uint8Array, contentType: string, attributes?: Record<string, string | number | boolean>): Promise<{ jobId: string }>;
}

/** Standard IPP boundary for network printing. A platform implementation supplies
 * HTTP/TLS and IPP encoding; vendor-specific code stays outside the core domain. */
export class IppPrinterAdapter {
  constructor(private readonly transport: IppTransport, private readonly printerUri: string) {}

  print(document: Uint8Array, contentType = 'application/pdf', copies = 1) {
    return this.transport.sendPrintJob(this.printerUri, document, contentType, { copies });
  }
}
