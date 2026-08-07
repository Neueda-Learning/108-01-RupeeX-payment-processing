import type { Metadata } from "next";
import { IBM_Plex_Mono, Space_Grotesk } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/sidebar";
import { Footer } from "@/components/footer";
import { BotChatWidget } from "@/components/bot-chat-widget";
import { ToastProvider } from "@/components/toast-provider";

const displaySans = Space_Grotesk({
  variable: "--font-display-sans",
  subsets: ["latin"],
});

const plexMono = IBM_Plex_Mono({
  variable: "--font-plex-mono",
  weight: ["400", "500"],
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "RupeeX | Payment Operations Console",
  description:
    "RupeeX gives operations teams clear views for admin, source account, and destination account payment flows.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${displaySans.variable} ${plexMono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-gradient-to-br from-slate-50 to-slate-100">
        <ToastProvider>
          <div className="flex min-h-screen">
            <Sidebar />
            <div className="flex flex-1 flex-col md:ml-56">
              <main className="flex-1">{children}</main>
              <Footer />
            </div>
          </div>
          <BotChatWidget />
        </ToastProvider>
      </body>
    </html>
  );
}
