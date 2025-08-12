"use client";

import React, { useState } from "react";

interface props {
  initialData?: deliveryAddressDetail | null;
  setIsView: React.Dispatch<React.SetStateAction<boolean>>;
  onFunc: Function;
}

export default function AddAddress({ initialData, setIsView, onFunc }: props) {
  const [postalCode, setPostalCode] = useState<string>(
    initialData ? initialData.postalCode : ""
  );

  const onChangePostalCode = (value: string) => {
    const sanitized = value.replace(/[^0-9-]/g, "");
    setPostalCode(sanitized);
  };

  const [address1, setAddress1] = useState<string>(
    initialData ? initialData.address1 : ""
  );

  return (
    <div className="cart-border mt-1">
      <div className="wf flex gap-05 bx ac pd-1">
        <div>郵便番号</div>
        <input
          type="text"
          value={postalCode}
          onChange={(e) => onChangePostalCode(e.target.value)}
        />
      </div>

      <div className="wf flex gap-05 bx ac pd-1">
        <div>詳細住所</div>
        <textarea
          value={address1}
          onChange={(e) => setAddress1(e.target.value)}
        />
      </div>
      <div className="pd-1 flex je gap-1 add-address-btns">
        <button className="cancel" onClick={() => setIsView(false)}>
          キャンセル
        </button>
        <button
          className="add"
          onClick={() => onFunc({ postalCode, address1 })}
        >
          {initialData ? "更新" : "追加"}
        </button>
      </div>
    </div>
  );
}
