import type { Metadata } from "next";
import { IBM_Plex_Mono, Space_Grotesk } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/sidebar";
import { Footer } from "@/components/footer";
import { BotChatWidget } from "@/components/bot-chat-widget";
import { ThemeProvider } from "@/lib/theme-provider";

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
      <body className="min-h-full flex flex-col text-slate-900 dark:text-slate-100 dark:bg-slate-950">
        <ThemeProvider>
          <Sidebar />
          <main className="flex-1 md:ml-64">{children}</main>
          <Footer />
          <BotChatWidget />
        </ThemeProvider>
      </body>
    </html>
  );
}
