export const newImageSizing = (url: string, num: number) => {
  return url.replace("128x128", `${num}x${num}`);
};
