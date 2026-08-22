export interface ScanPage { index: number; uri: string; width?: number; height?: number; }

export interface ImageProcessor {
  id: string;
  process(page: ScanPage, options: Record<string, unknown>): Promise<ScanPage>;
}

export interface ScanPipelineOptions {
  brightness: number;
  contrast: number;
  autoCrop: boolean;
  deskew: boolean;
  orientation: 'portrait' | 'landscape' | 'auto';
}

export class ScanPipeline {
  constructor(private readonly processors: ImageProcessor[] = []) {}

  async process(pages: ScanPage[], options: ScanPipelineOptions): Promise<ScanPage[]> {
    let current = [...pages];
    for (const processor of this.processors) {
      current = await Promise.all(current.map((page) => processor.process(page, options)));
    }
    return current;
  }
}

export interface Exporter {
  format: 'jpeg' | 'png' | 'pdf';
  export(pages: ScanPage[], metadata?: Record<string, string>): Promise<string>;
}

export class ExportManager {
  constructor(private readonly exporters: Exporter[]) {}

  async export(format: Exporter['format'], pages: ScanPage[], metadata?: Record<string, string>) {
    const exporter = this.exporters.find((item) => item.format === format);
    if (!exporter) throw new Error(`No exporter registered for ${format}`);
    return exporter.export(pages, metadata);
  }
}
