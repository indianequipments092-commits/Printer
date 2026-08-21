export type PrintBridgeCommand =
  | { command: 'devices'; transport?: 'usb' | 'network' | 'all' }
  | { command: 'scan'; deviceId: string; settings?: Record<string, unknown> }
  | { command: 'print'; deviceId: string; source: string }
  | { command: 'job'; jobId: string }
  | { command: 'cancel'; jobId: string };

export interface CommandResult<T = unknown> {
  ok: boolean;
  data?: T;
  error?: { code: string; message: string; recoverable: boolean };
}

export function parseCommand(input: string[]): PrintBridgeCommand {
  const [command, ...args] = input;
  switch (command) {
    case 'devices':
      return { command, transport: (args[0] as 'usb' | 'network' | 'all' | undefined) ?? 'all' };
    case 'scan':
      if (!args[0]) throw new Error('scan requires <deviceId>');
      return { command, deviceId: args[0] };
    case 'print':
      if (!args[0] || !args[1]) throw new Error('print requires <deviceId> <source>');
      return { command, deviceId: args[0], source: args[1] };
    case 'job':
      if (!args[0]) throw new Error('job requires <jobId>');
      return { command, jobId: args[0] };
    case 'cancel':
      if (!args[0]) throw new Error('cancel requires <jobId>');
      return { command, jobId: args[0] };
    default:
      throw new Error(`Unknown PrintBridge command: ${command ?? '<empty>'}`);
  }
}
