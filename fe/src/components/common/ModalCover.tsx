import React, { useEffect } from "react";

interface props {
  children: React.ReactNode;
}

export default function ModalCover({ children }: props) {
  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "auto";
    };
  }, []);
  return <div className="fix modal-cover flex ac jc">{children}</div>;
}
