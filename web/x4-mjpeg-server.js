import { createServer } from 'node:http';
import { spawn, spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..');
const previewRoot = resolve(repoRoot, 'tools/insta360_x4_preview');
const previewBin = resolve(previewRoot, 'build/insta360_x4_preview');
const cameraSdkLib = resolve(repoRoot, 'CameraSDK/lib');
const port = Number(process.env.X4_MJPEG_PORT || 8787);
const cameraIp = process.env.X4_IP || '192.168.42.1';
const cameraPort = process.env.X4_PORT || '6666';
const codec = process.env.X4_CODEC || 'h264';

let previewProcess;
let ffmpegProcess;
let latestFrame;
let frameBuffer = Buffer.alloc(0);
const clients = new Set();

function buildPreviewTool() {
  const result = spawnSync(resolve(previewRoot, 'build.sh'), {
    cwd: repoRoot,
    stdio: 'inherit',
  });

  if (result.status !== 0) {
    throw new Error('failed to build insta360_x4_preview');
  }
}

function stopPipeline() {
  if (previewProcess && !previewProcess.killed) {
    previewProcess.kill('SIGTERM');
  }
  if (ffmpegProcess && !ffmpegProcess.killed) {
    ffmpegProcess.kill('SIGTERM');
  }
  previewProcess = undefined;
  ffmpegProcess = undefined;
}

function broadcastFrame(frame) {
  latestFrame = frame;
  for (const response of clients) {
    response.write(`--x4frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.length}\r\n\r\n`);
    response.write(frame);
    response.write('\r\n');
  }
}

function parseJpegFrames(chunk) {
  frameBuffer = Buffer.concat([frameBuffer, chunk]);

  while (true) {
    const start = frameBuffer.indexOf(Buffer.from([0xff, 0xd8]));
    if (start < 0) {
      frameBuffer = Buffer.alloc(0);
      return;
    }

    const end = frameBuffer.indexOf(Buffer.from([0xff, 0xd9]), start + 2);
    if (end < 0) {
      if (start > 0) {
        frameBuffer = frameBuffer.subarray(start);
      }
      return;
    }

    const frame = frameBuffer.subarray(start, end + 2);
    frameBuffer = frameBuffer.subarray(end + 2);
    broadcastFrame(frame);
  }
}

function ensurePipeline() {
  if (previewProcess && !previewProcess.killed && ffmpegProcess && !ffmpegProcess.killed) {
    return;
  }

  buildPreviewTool();

  previewProcess = spawn(previewBin, ['--stdout', '--ip', cameraIp, '--port', cameraPort], {
    cwd: repoRoot,
    env: {
      ...process.env,
      LD_LIBRARY_PATH: `${cameraSdkLib}:${process.env.LD_LIBRARY_PATH || ''}`,
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  ffmpegProcess = spawn(
    'ffmpeg',
    [
      '-hide_banner',
      '-loglevel',
      'warning',
      '-fflags',
      'nobuffer',
      '-flags',
      'low_delay',
      '-probesize',
      '32',
      '-analyzeduration',
      '0',
      '-f',
      codec,
      '-i',
      'pipe:0',
      '-vf',
      'scale=1280:-2',
      '-q:v',
      '5',
      '-f',
      'image2pipe',
      '-vcodec',
      'mjpeg',
      'pipe:1',
    ],
    { stdio: ['pipe', 'pipe', 'pipe'] },
  );

  previewProcess.stdout.pipe(ffmpegProcess.stdin);
  previewProcess.stderr.on('data', (data) => process.stderr.write(`[x4] ${data}`));
  ffmpegProcess.stderr.on('data', (data) => process.stderr.write(`[ffmpeg] ${data}`));
  ffmpegProcess.stdout.on('data', parseJpegFrames);

  previewProcess.on('exit', (code, signal) => {
    process.stderr.write(`[x4] preview exited code=${code} signal=${signal}\n`);
    if (ffmpegProcess && !ffmpegProcess.killed) {
      ffmpegProcess.kill('SIGTERM');
    }
  });

  ffmpegProcess.on('exit', (code, signal) => {
    process.stderr.write(`[ffmpeg] exited code=${code} signal=${signal}\n`);
  });
}

const server = createServer((request, response) => {
  response.setHeader('Access-Control-Allow-Origin', '*');

  if (request.url === '/health') {
    response.writeHead(200, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({ ok: true, clients: clients.size, hasFrame: Boolean(latestFrame) }));
    return;
  }

  if (request.url === '/latest.jpg') {
    ensurePipeline();
    if (!latestFrame) {
      response.writeHead(503, { 'Content-Type': 'text/plain' });
      response.end('No X4 frame yet');
      return;
    }
    response.writeHead(200, { 'Content-Type': 'image/jpeg', 'Cache-Control': 'no-store' });
    response.end(latestFrame);
    return;
  }

  if (request.url === '/x4.mjpeg') {
    ensurePipeline();
    response.writeHead(200, {
      'Content-Type': 'multipart/x-mixed-replace; boundary=x4frame',
      'Cache-Control': 'no-store, no-cache, must-revalidate, proxy-revalidate',
      Connection: 'close',
      Pragma: 'no-cache',
    });

    clients.add(response);
    if (latestFrame) {
      response.write(`--x4frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${latestFrame.length}\r\n\r\n`);
      response.write(latestFrame);
      response.write('\r\n');
    }

    request.on('close', () => {
      clients.delete(response);
    });
    return;
  }

  response.writeHead(200, { 'Content-Type': 'text/plain' });
  response.end('Insta360 X4 MJPEG bridge: /x4.mjpeg /latest.jpg /health\n');
});

process.on('SIGINT', () => {
  stopPipeline();
  process.exit(0);
});
process.on('SIGTERM', () => {
  stopPipeline();
  process.exit(0);
});

server.listen(port, '0.0.0.0', () => {
  console.log(`X4 MJPEG bridge listening on http://localhost:${port}/x4.mjpeg`);
});
