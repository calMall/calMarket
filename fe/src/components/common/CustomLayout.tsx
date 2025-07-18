import React from "react";

export default function CustomLayout({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`layout ${className ? className : ""}`}>{children}</div>
  );
}
