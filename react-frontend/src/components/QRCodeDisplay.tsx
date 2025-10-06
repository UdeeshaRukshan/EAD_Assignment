import React, { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';

interface QRCodeDisplayProps {
  data: string;
  size?: number;
  className?: string;
  showDownload?: boolean;
  label?: string;
  booking?: any; // Booking object with full details
  stationInfo?: {
    name: string;
    address: string;
    connectorInfo: string;
  };
}

const QRCodeDisplay: React.FC<QRCodeDisplayProps> = ({
  data,
  size = 200,
  className = '',
  showDownload = true,
  label = 'QR Code',
  booking,
  stationInfo
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  const getStatusText = (status: any): string => {
    if (typeof status === 'number') {
      const statusMap: { [key: number]: string } = {
        0: 'PENDING',
        1: 'CONFIRMED',
        2: 'IN_PROGRESS',
        3: 'COMPLETED',
        4: 'CANCELLED',
        5: 'NO_SHOW'
      };
      return statusMap[status] || 'UNKNOWN';
    }
    return status || 'PENDING';
  };

  useEffect(() => {
    const generateQR = async () => {
      if (!canvasRef.current || !data) return;

      try {
        setIsLoading(true);
        setError('');

        // Decode the base64 data if it's encoded
        let qrData = data;
        let parsedData: any = null;
        
        try {
          const decoded = atob(data);
          
          // Try to parse as JSON first (new format)
          try {
            parsedData = JSON.parse(decoded);
            
            // If it's our structured booking data, use compact format
            if (parsedData.Type === 'EV_CHARGING_BOOKING' || parsedData.type === 'EV_BOOKING') {
              qrData = JSON.stringify(parsedData); // Compact format, no pretty printing
            } else {
              qrData = decoded;
            }
          } catch {
            // If JSON parsing fails, check if it's the old BOOKING: format
            if (decoded.startsWith('BOOKING:')) {
              const bookingId = decoded.replace('BOOKING:', '');
              // Convert old format to comprehensive structured format
              const enhancedFormat = {
                type: "EV_BOOKING",
                id: bookingId,
                ts: new Date().toISOString().split('T')[0],
                v: "1.0",
                // Add comprehensive booking details if available
                ...(booking && {
                  station: stationInfo?.name || "N/A",
                  address: stationInfo?.address || "N/A",
                  connector: stationInfo?.connectorInfo || "N/A",
                  start: booking.startTime ? new Date(booking.startTime).toLocaleString() : "N/A",
                  end: booking.endTime ? new Date(booking.endTime).toLocaleString() : "N/A",
                  duration: booking.startTime && booking.endTime ? 
                    Math.round((new Date(booking.endTime).getTime() - new Date(booking.startTime).getTime()) / (1000 * 60)) + " min" : "N/A",
                  created: booking.createdAt ? new Date(booking.createdAt).toLocaleString() : "N/A",
                  status: getStatusText(booking.status)
                })
              };
              qrData = JSON.stringify(enhancedFormat);
            } else {
              qrData = decoded;
            }
          }
        } catch {
          // If base64 decoding fails, check if it's plain text old format
          if (data.startsWith('BOOKING:')) {
            const bookingId = data.replace('BOOKING:', '');
            const enhancedFormat = {
              type: "EV_BOOKING",
              id: bookingId,
              ts: new Date().toISOString().split('T')[0],
              v: "1.0",
              // Add comprehensive booking details if available
              ...(booking && {
                station: stationInfo?.name || "N/A",
                address: stationInfo?.address || "N/A",
                connector: stationInfo?.connectorInfo || "N/A",
                start: booking.startTime ? new Date(booking.startTime).toLocaleString() : "N/A",
                end: booking.endTime ? new Date(booking.endTime).toLocaleString() : "N/A",
                duration: booking.startTime && booking.endTime ? 
                  Math.round((new Date(booking.endTime).getTime() - new Date(booking.startTime).getTime()) / (1000 * 60)) + " min" : "N/A",
                created: booking.createdAt ? new Date(booking.createdAt).toLocaleString() : "N/A",
                status: getStatusText(booking.status)
              })
            };
            qrData = JSON.stringify(enhancedFormat);
          } else {
            // Use the original data as fallback
            qrData = data;
          }
        }

        await QRCode.toCanvas(canvasRef.current, qrData, {
          width: size,
          margin: 2,
          color: {
            dark: '#000000',
            light: '#FFFFFF'
          }
        });
      } catch (err) {
        console.error('Error generating QR code:', err);
        setError('Failed to generate QR code');
      } finally {
        setIsLoading(false);
      }
    };

    generateQR();
  }, [data, size]);

  const downloadQR = () => {
    if (!canvasRef.current) return;

    const link = document.createElement('a');
    link.download = `qr-code-${Date.now()}.png`;
    link.href = canvasRef.current.toDataURL();
    link.click();
  };

  const copyToClipboard = async () => {
    if (!canvasRef.current) return;

    try {
      const canvas = canvasRef.current;
      canvas.toBlob(async (blob) => {
        if (blob) {
          const item = new ClipboardItem({ 'image/png': blob });
          await navigator.clipboard.write([item]);
          alert('QR code copied to clipboard!');
        }
      });
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
      alert('Failed to copy QR code to clipboard');
    }
  };

  if (error) {
    return (
      <div className={`text-center p-4 bg-red-50 border border-red-200 rounded-lg ${className}`}>
        <div className="text-red-600 text-sm">⚠️ {error}</div>
      </div>
    );
  }

  return (
    <div className={`text-center ${className}`}>
      <div className="relative inline-block">
        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-100 rounded-lg">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        )}
        <canvas
          ref={canvasRef}
          className={`border border-gray-300 rounded-lg ${isLoading ? 'opacity-0' : 'opacity-100'} transition-opacity`}
        />
      </div>
      
      {label && (
        <p className="text-sm text-gray-600 mt-2 font-medium">{label}</p>
      )}
      
      {showDownload && !isLoading && (
        <div className="flex justify-center space-x-2 mt-3">
          <button
            onClick={downloadQR}
            className="px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
          >
            Download
          </button>
          <button
            onClick={copyToClipboard}
            className="px-3 py-1 text-xs bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors"
          >
            Copy
          </button>
        </div>
      )}
    </div>
  );
};

export default QRCodeDisplay;